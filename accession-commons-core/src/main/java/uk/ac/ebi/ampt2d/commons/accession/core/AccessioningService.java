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
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;

import java.util.List;

public interface AccessioningService<MODEL, HASH, ACCESSION> {

    List<AccessionWrapper<MODEL, HASH, ACCESSION>> getOrCreate(List<? extends MODEL> messages)
            throws AccessionCouldNotBeGeneratedException;

    List<AccessionWrapper<MODEL, HASH, ACCESSION>> get(List<? extends MODEL> accessionedObjects);

    /**
     * Finds last version of all valid accessions with their possible data model representations.
     *
     * @param accessions
     * @return
     */
    List<AccessionWrapper<MODEL, HASH, ACCESSION>> getByAccessions(List<ACCESSION> accessions);

    AccessionWrapper<MODEL, HASH, ACCESSION> getByAccessionAndVersion(ACCESSION accessions, int version)
    throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException;

    /**
     * Updates a specific patch version of an accessioned object. It does not create a new version / patch
     *
     * @param accession
     * @param version
     * @param message
     * @return Accession with complete patch information
     * @throws AccessionDoesNotExistException when the accession has never existed.
     * @throws HashAlreadyExistsException     when another accessioned object exists already with the same hash
     * @throws AccessionDeprecatedException   when the accession exists but has been deprecated
     * @throws AccessionMergedException       when the accession exists but has been merged into another accession
     */
    AccessionVersionsWrapper<MODEL, HASH, ACCESSION> update(ACCESSION accession, int version, MODEL message)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionDeprecatedException,
            AccessionMergedException;

    /**
     * Creates a new patch version of an accession.
     *
     * @param accession
     * @param message
     * @return Accession with complete patch information
     * @throws AccessionDoesNotExistException when the accession has never existed.
     * @throws HashAlreadyExistsException     when another accessioned object exists already with the same hash
     * @throws AccessionDeprecatedException   when the accession exists but has been deprecated
     * @throws AccessionMergedException       when the accession exists but has been merged into another accession
     */
    AccessionVersionsWrapper<MODEL, HASH, ACCESSION> patch(ACCESSION accession, MODEL message)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionDeprecatedException,
            AccessionMergedException;

    /**
     * Deprecates an accession
     *
     * @param accession
     * @param reason
     * @return Accession with complete patch information
     * @throws AccessionDoesNotExistException when the accession has never existed.
     * @throws AccessionDeprecatedException   when the accession exists but has been deprecated
     * @throws AccessionMergedException       when the accession exists but has been merged into another accession
     */
    void deprecate(ACCESSION accession, String reason) throws AccessionMergedException, AccessionDoesNotExistException,
            AccessionDeprecatedException;

    /**
     * Merges an accession into another
     *
     * @param accessionOrigin
     * @param accessionDestination
     * @param reason
     * @throws AccessionDoesNotExistException when either accession has never existed.
     * @throws AccessionDeprecatedException   when either accession exists but has been deprecated
     * @throws AccessionMergedException       when either accession exists but has been merged into another accession
     */
    void merge(ACCESSION accessionOrigin, ACCESSION accessionDestination, String reason) throws AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException;

}
