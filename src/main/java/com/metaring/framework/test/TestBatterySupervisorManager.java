/**
 *    Copyright 2019 MetaRing s.r.l.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.metaring.framework.test;

class TestBatterySupervisorManager {

    private static boolean STARTED = false;

    @SuppressWarnings("unchecked")
    static CoreTestsBatterySupervisor initSupervisor() {
        CoreTestsBatterySupervisor coreTestSupervisor = null;
        if (STARTED) {
            RuntimeException e = new RuntimeException("Supervisor already initialized!");
            e.printStackTrace();
            throw e;
        }
        STARTED = true;

        try {
            Class<? extends CoreTestsBatterySupervisor> coreTestSupervisorClass = null;
            try {
                coreTestSupervisorClass = (Class<? extends CoreTestsBatterySupervisor>) Class.forName(CoreTestsBatterySupervisor.class.getName() + "Impl");
            }
            catch (Exception ex) {
            }
            if (coreTestSupervisorClass != null) {
                coreTestSupervisor = coreTestSupervisorClass.newInstance();
                coreTestSupervisor.init();
                final CoreTestsBatterySupervisor coreTestSupervisorFinal = coreTestSupervisor;
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        try {
                            coreTestSupervisorFinal.end();
                        }
                        catch (Exception e) {
                            System.err.println("Exception while ending Tests Battery Supervisor:\n\n");
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception while initializing Tests Battery Supervisor", e);
        }
        return coreTestSupervisor;
    }

}
