package org.cratedb.action.sql;

import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.object.ObjectMapper;
import org.elasticsearch.index.mapper.object.RootObjectMapper;

import java.util.*;

public class TableExecutionContext implements ITableExecutionContext {

        private final DocumentMapper documentMapper;
        private final MappingMetaData mappingMetaData;
        private final String tableName;

        TableExecutionContext(String name, DocumentMapper documentMapper, MappingMetaData mappingMetaData) {
            this.documentMapper = documentMapper;
            this.mappingMetaData = mappingMetaData;
            this.tableName = name;
        }

        public DocumentMapper mapper() {
            return documentMapper;
        }

        /**
         *
         * @param name the name of the column
         * @param value the value to be mapped
         * @return the value converted to the proper type
         */
        public Object mappedValue(String name, Object value){
            FieldMapper fieldMapper = documentMapper.mappers().smartNameFieldMapper(name);
            if (fieldMapper != null) {
                return fieldMapper.value(value);
            }
            return value;
        }

        /**
         * Returns the ``primary key`` column names defined at index creation under the ``_meta``
         * key. If not defined, return empty list.
         *
         * @return a list of primary key column names
         */
        public List<String> primaryKeys() {
            List<String> pks = new ArrayList<String>();
            Object srcPks = documentMapper.meta().get("primary_keys");
            if (srcPks instanceof String) {
                pks.add((String)srcPks);
            } else if (srcPks instanceof List) {
                pks.addAll((List)srcPks);
            }

            return pks;
        }

        /**
         * Returns the ``primary key`` column names defined at index creation under the ``_meta``
         * key. If none defined, add ``_id`` as primary key(Default).
         *
         * @return a list of primary key column names
         */
        public List<String> primaryKeysIncludingDefault() {
            List<String> primaryKeys = primaryKeys();
            if (primaryKeys.isEmpty()) {
                primaryKeys.add("_id"); // Default Primary Key (only for optimization, not for consistency checks)
            }
            return primaryKeys;
        }


        /**
         * returns all columns defined in the mapping as a sorted sequence
         * to be used in "*" selects.
         *
         * @return a sequence of column names
         */
        public Iterable<String> allCols() {
            Set<String> res = new TreeSet<String>();
            for (FieldMapper m : documentMapper.mappers()) {
                String name = m.names().name();
                // don't add internal and sub-object field names
                if (!name.startsWith("_") && !m.names().sourcePath().contains(".")) {
                    res.add(name);
                }
            }

            // add object type field names
            Map<String, ObjectMapper> objectMappers = documentMapper.objectMappers();
            for (Map.Entry<String, ObjectMapper> entry : objectMappers.entrySet()) {
                ObjectMapper mapper = entry.getValue();
                if (mapper instanceof RootObjectMapper) {
                    continue;
                }
                res.add(entry.getKey());
            }
            return res;
        }

        /**
         * Check if given name is equal to defined routing name.
         *
         * @param name
         * @return
         */
        public Boolean isRouting(String name) {
            String routingPath = mappingMetaData.routing().path();
            if (routingPath == null) {
                // the primary key(s) values are saved under _id, so they are used as default
                // routing values
                if (primaryKeys().contains(name)) {
                    return true;
                }
                routingPath = "_id";
            }
            return routingPath.equals(name);
        }
}
