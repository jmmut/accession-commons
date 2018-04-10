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
package uk.ac.ebi.ampt2d.test.persistence;

import uk.ac.ebi.ampt2d.commons.accession.generators.ModelHashAccession;
import uk.ac.ebi.ampt2d.commons.accession.persistence.IAccessionedObject;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.accession.entity.AccessionedStringEntity;
import uk.ac.ebi.ampt2d.test.TestModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TestEntity extends AccessionedStringEntity implements TestModel  {

    private String something;

    TestEntity() {
        super(null, null);
    }

    public TestEntity(ModelHashAccession<TestModel, String, String> triple) {
        this(triple.accession(),triple.hash(),triple.model().getSomething());
    }

    public TestEntity(String accession, String hashedMessage, String something) {
        super(hashedMessage,accession);
        this.something = something;
    }

    @Override
    public String getSomething() {
        return something;
    }

}
