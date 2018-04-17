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
package uk.ac.ebi.ampt2d.commons.accession.core.exceptions;

import uk.ac.ebi.ampt2d.commons.accession.core.AccessionWrapper;

import java.util.ArrayList;
import java.util.List;

public class MissingUnsavedAccessions extends RuntimeException {

    public <MODEL, HASH, ACCESSION> MissingUnsavedAccessions(List<AccessionWrapper<MODEL, HASH, ACCESSION>> dbAccessions,
                                                             List<AccessionWrapper<MODEL, HASH, ACCESSION>>
                                                                     saveFailedAccessions) {
        super("Unsaved objects could not be found: " + generateMessage(dbAccessions, saveFailedAccessions));
    }

    private static <MODEL, HASH, ACCESSION> String generateMessage(List<AccessionWrapper<MODEL, HASH, ACCESSION>>
                                                                           dbAccessions,
                                                                   List<AccessionWrapper<MODEL, HASH, ACCESSION>>
                                                                           unsavedObjects) {
        List<MODEL> missingObjects = new ArrayList<>();

// TODO REDO FUNCTION
        return missingObjects.toString();
    }
}
