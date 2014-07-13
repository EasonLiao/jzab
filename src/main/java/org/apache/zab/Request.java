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

import org.apache.zab.proto.ZabMessage.Message;

/**
 * Encapsulates the request needs to be processed by RequestProcessor.
 */
public class Request {
  private final String serverId;
  private final Message message;

  public static final Request REQUEST_OF_DEATH = new Request(null, null);

  public Request(String serverId, Message message) {
    this.serverId = serverId;
    this.message = message;
  }

  public String getServerId() {
    return this.serverId;
  }

  public Message getMessage() {
    return this.message;
  }
}