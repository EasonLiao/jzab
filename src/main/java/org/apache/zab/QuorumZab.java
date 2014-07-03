/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zab;

import java.io.IOException;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quorum zab implementation. This class manages the quorum zab servers.
 * For each server, it can be in one of three states :
 *   ELECTING, FOLLOWING, LEADING.
 * Among all the nodes, there's only one server can be established leader.
 */
public class QuorumZab extends Zab {

  protected final Participant participant;

  private static final Logger LOG = LoggerFactory.getLogger(QuorumZab.class);

  public QuorumZab(StateMachine stateMachine,
                   Properties prop,
                   Zxid lastCommittedZxid)
      throws IOException {
    super(stateMachine, prop);

    this.participant = new Participant(this.config,
                                       stateMachine,
                                       lastCommittedZxid);

    Executors.newSingleThreadExecutor(DaemonThreadFactory.FACTORY)
             .execute(this.participant);
  }

  QuorumZab(StateMachine stateMachine,
            StateChangeCallback cb,
            TestState initialState,
            Zxid lastCommittedZxid) throws IOException {

    super(stateMachine, initialState.prop);

    this.participant = new Participant(stateMachine,
                                       cb,
                                       initialState,
                                       lastCommittedZxid);

    Executors.newSingleThreadExecutor(DaemonThreadFactory.FACTORY)
             .execute(this.participant);
  }

  @Override
  public void send(ByteBuffer message) throws IOException {
    this.participant.send(message);
  }

  @Override
  public void trimLogTo(Zxid zxid) {
  }


  /**
   * Interface of callbacks which will be called when phase change happens.
   * Used for testing purpose.
   *
   * Phase changes :
   *
   *        leaderDiscovering - leaderSynchronizating - leaderBroadcasting
   *        /
   * electing
   *        \
   *        followerDiscovering - followerSynchronizating - followerBroadcasting
   *
   */
  interface StateChangeCallback {

    /**
     * Will be called when entering electing phase.
     */
    void electing();

    /**
     * Will be called when entering discovering phase of leader.
     *
     * @param electedLeader the elected leader.
     */
    void leaderDiscovering(String electedLeader);

    /**
     * Will be called when entering discovery phase of follower.
     *
     * @param electedLeader the elected leader of this follower.
     */
    void followerDiscovering(String electedLeader);


    /**
     * Will be called on leader side when the owner of initial history is
     * chosen.
     *
     * @param server the id of the server whose history is selected for
     * synchronization.
     * @param aEpoch the acknowledged epoch of the node whose initial history
     * is chosen for synchronization.
     * @param zxid the last transaction id of the node whose initial history
     * is chosen for synchronization.
     */
    void initialHistoryOwner(String server, int aEpoch, Zxid zxid);

    /**
     * Will be called when entering synchronization phase of leader.
     *
     * @param epoch the established epoch.
     */
    void leaderSynchronizating(int epoch);

    /**
     * Will be called when entering synchronization phase of follower.
     *
     * @param epoch the established epoch.
     */
    void followerSynchronizating(int epoch);

    /**
     * Will be called when entering broadcasting phase of leader.
     *
     * @param epoch the acknowledged epoch (f.a).
     * @param history the initial history (f.h) of broadcasting phase.
     */
    void leaderBroadcasting(int epoch, List<Transaction> history);

    /**
     * Will be called when entering broadcasting phase of follower.
     *
     * @param epoch the current epoch (f.a).
     * @param history the initial history (f.h) of broadcasting phase.
     */
    void followerBroadcasting(int epoch, List<Transaction> history);
  }

  /**
   * Used for initializing the state of QuorumZab for testing purpose.
   */
  static class TestState {
    Properties prop = new Properties();

    File logDir = null;

    private final File fAckEpoch;

    private final File fProposedEpoch;

    Log log = null;

    /**
     * Creates the TestState object. It should be passed to QuorumZab to
     * initialize its state.
     *
     * @param serverId the server id of the QuorumZab.
     * @param servers the server list of the ensemble.
     * @param baseLogDir the base log directory of all the peers in ensemble.
     * The specific log directory of each peer is baseLogDir/serverId.
     * For example, if the baseLogDir is /tmp/log and the serverId is server1,
     * then the log directory for this server is /tmp/log/server1.
     */
    public TestState(String serverId, String servers, File baseLogDir) {
      this.prop.setProperty("serverId", serverId);
      this.prop.setProperty("servers", servers);
      this.logDir = new File(baseLogDir, serverId);

      // Creates its log directory.
      if(!this.logDir.mkdir()) {
        LOG.warn("Creating log directory {} failed, already exists?",
                 this.logDir.getAbsolutePath());
      }

      this.prop.setProperty("logdir", logDir.getAbsolutePath());
      this.fAckEpoch = new File(this.logDir, "AckEpoch");
      this.fProposedEpoch = new File(this.logDir, "ProposedEpoch");

    }

    public TestState(ZabConfig config) {
      this(config.getServerId(),
           config.prop.getProperty("servers"),
           new File(config.getLogDir()));
    }

    TestState setProposedEpoch(int epoch) throws IOException {
      FileUtils.writeIntToFile(epoch, this.fProposedEpoch);
      return this;
    }

    TestState setAckEpoch(int epoch) throws IOException {
      FileUtils.writeIntToFile(epoch, this.fAckEpoch);
      return this;
    }

    TestState setLog(Log tlog) {
      this.log = tlog;
      return this;
    }

    Log getLog() {
      return this.log;
    }
  }
}
