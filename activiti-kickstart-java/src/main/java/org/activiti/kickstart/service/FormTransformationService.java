/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.activiti.kickstart.service;

import org.activiti.kickstart.dto.KickstartForm;

/**
 * Implementations of this interface are responsible for transforming a {@link KickstartForm} to
 * something that is executable by the targetted system.
 * 
 * The reason why the transformation of the forms is separated from the {@link TransformationService},
 * is because generally you'll want the Kickstart workflows to be transformed to valid BPMN 2.0,
 * while the forms are often very specific and non standard (eg. Activiti standalone vs embedded in Alfresco).
 * 
 * @author jbarrez
 */
public interface FormTransformationService {

}
