/**
 * Copyright (C) 2012 LinkedIn Inc <opensource@linkedin.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.helix.participant;

import com.linkedin.helix.messaging.handling.MessageHandlerFactory;
import com.linkedin.helix.participant.statemachine.StateModel;
import com.linkedin.helix.participant.statemachine.StateModelFactory;

/**
 * Helix participant manager uses this class to register/remove state model factory
 * State model factory creates state model that handles state transition messages
 */
public interface StateMachineEngine extends MessageHandlerFactory
{
  /**
   * Register a default state model factory for a state model definition
   * A state model definition could be, for example: 
   * "MasterSlave", "OnlineOffline", "LeaderStandby", etc.
   * @param stateModelDef
   * @param factory
   * @return
   */
  public boolean registerStateModelFactory(String stateModelDef,
      StateModelFactory<? extends StateModel> factory);

  // public boolean registerStateModelFactory(String stateModelDef,
  // String resourceGroupName,
  // StateModelFactory<? extends StateModel> factory);

  /**
   * Register a state model factory with a name for a state model definition
   * @param stateModelDef
   * @param factory
   * @param factoryName
   * @return
   */
  public boolean registerStateModelFactory(String stateModelDef,
      StateModelFactory<? extends StateModel> factory, String factoryName);

  /**
   * Remove the default state model factory for a state model definition
   * @param stateModelDef
   * @param factory
   * @return
   */
  public boolean removeStateModelFactory(String stateModelDef,
      StateModelFactory<? extends StateModel> factory);

  /**
   * Remove the state model factory with a name for a state model definition
   * @param stateModelDef
   * @param factory
   * @param factoryName
   * @return
   */
  public boolean removeStateModelFactory(String stateModelDef,
      StateModelFactory<? extends StateModel> factory, String factoryName);

}
