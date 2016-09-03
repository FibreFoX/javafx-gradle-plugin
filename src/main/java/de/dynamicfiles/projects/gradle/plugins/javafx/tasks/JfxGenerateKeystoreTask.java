/*
 * Copyright 2016 Danny Althoff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dynamicfiles.projects.gradle.plugins.javafx.tasks;

import de.dynamicfiles.projects.gradle.plugins.javafx.tasks.workers.JfxGenerateKeystoreWorker;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.TaskAction;

/**
 *
 * @author Danny Althoff
 */
public class JfxGenerateKeystoreTask extends AbstractTask {

    public static final String JFX_TASK_NAME = "jfxGenerateKeyStore";

    @TaskAction
    public void jfxgeneratekeystore() {
        new JfxGenerateKeystoreWorker().jfxgeneratekeystore(this.getProject());
    }

}
