/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
 *
 */
package uk.ac.ebi.ampt2d.commons.accession.core;

import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;

import java.util.List;

public interface AccessioningService<MODEL, HASH, ACCESSION> {

    List<AccessionModel<MODEL, HASH, ACCESSION>> getOrCreateAccessions(List<? extends MODEL> messages)
            throws AccessionCouldNotBeGeneratedException;

    List<AccessionModel<MODEL, HASH, ACCESSION>> getAccessions(List<? extends MODEL> accessionedObjects);

    List<AccessionModel<MODEL, HASH, ACCESSION>> getByAccessions(List<ACCESSION> accessions,
                                                                        boolean hideDeprecated);
}
