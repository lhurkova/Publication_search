/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cz.cuni.mff.hurkovalu.publication_search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing an author of a publication with given last name and forenames (initials).
 * @author Lucie Hurkova
 */
public record Author(String lastName, String[] foreNames) implements Serializable {
    @Override
    public boolean equals(Object o) {
        if (o instanceof Author secondAuthor) {
            if (lastName != null && foreNames != null) {
                return (this.lastName.equals(secondAuthor.lastName())) &&
                        Arrays.equals(this.foreNames, secondAuthor.foreNames());
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        List<String> allFields = new ArrayList<>(Arrays.asList(foreNames));
        allFields.add(lastName);
        return Arrays.hashCode(allFields.toArray());
    }
}
