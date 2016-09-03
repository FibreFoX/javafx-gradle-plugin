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
package de.dynamicfiles.projects.gradle.plugins.javafx.tasks.workers;

import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.UnsupportedPlatformException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;

/**
 *
 * @author Danny Althoff
 */
public class JfxListBundlersWorker extends JfxAbstractWorker {

    public void jfxlistbundlers(Project project) {
        Logger logger = project.getLogger();

        Bundlers bundlers = Bundlers.createBundlersInstance();
        logger.info("Available bundlers:");
        logger.info("-------------------");
        Map<String, ? super Object> dummyParams = new HashMap<>();
        bundlers.getBundlers().stream().forEach((bundler) -> {
            try{
                bundler.validate(dummyParams);
            } catch(UnsupportedPlatformException ex){
                return;
            } catch(ConfigException ex){
                // NO-OP
                // bundler is supported on this OS
            }

            logger.lifecycle("ID: " + bundler.getID());
            logger.lifecycle("Name: " + bundler.getName());
            logger.lifecycle("Description: " + bundler.getDescription());

            Collection<BundlerParamInfo<?>> bundleParameters = bundler.getBundleParameters();
            Optional.ofNullable(bundleParameters).ifPresent(nonNullBundleArguments -> {
                logger.info("Available bundle arguments: ");
                nonNullBundleArguments.stream().forEach(bundleArgument -> {
                    logger.info("\t\tArgument ID: " + bundleArgument.getID());
                    logger.info("\t\tArgument Type: " + bundleArgument.getValueType().getName());
                    logger.info("\t\tArgument Name: " + bundleArgument.getName());
                    logger.info("\t\tArgument Description: " + bundleArgument.getDescription());
                    logger.info("");
                });
            });
            logger.lifecycle("-------------------");
        });

        if( !logger.isEnabled(LogLevel.INFO) ){
            logger.lifecycle("For more information, please use --info parameter.");
        }
    }

}
