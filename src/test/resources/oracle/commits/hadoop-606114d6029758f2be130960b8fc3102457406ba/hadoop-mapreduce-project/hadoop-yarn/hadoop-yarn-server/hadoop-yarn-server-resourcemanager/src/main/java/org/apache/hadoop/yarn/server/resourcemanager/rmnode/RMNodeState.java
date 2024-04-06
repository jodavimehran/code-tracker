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

package org.apache.hadoop.yarn.server.resourcemanager.rmnode;

import org.apache.hadoop.yarn.api.records.NodeState;

//TODO yarn.api.records.NodeState is a clone of RMNodeState made for MR-3353. 
// In a subsequent patch RMNodeState should be replaced with NodeState
public enum RMNodeState {
  NEW, RUNNING, UNHEALTHY, DECOMMISSIONED, LOST, REBOOTED;
  
  public static NodeState toNodeState(RMNodeState state) {
    switch(state) {
    case NEW:
      return NodeState.NEW;
    case RUNNING:
      return NodeState.RUNNING;
    case UNHEALTHY:
      return NodeState.UNHEALTHY;
    case DECOMMISSIONED:
      return NodeState.DECOMMISSIONED;
    case LOST:
      return NodeState.LOST;
    case REBOOTED:
      return NodeState.REBOOTED;
    }
    return null;
  }
};
