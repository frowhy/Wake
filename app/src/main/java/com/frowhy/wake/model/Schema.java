package com.frowhy.wake.model;

import java.util.List;

/**
 * Wake
 * Created by frowhy on 2017/3/14.
 */

public class Schema {
    private List<SchemasBean> schemas;

    public List<SchemasBean> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<SchemasBean> schemas) {
        this.schemas = schemas;
    }

    public static class SchemasBean {
        /**
         * schema : mqq
         * package_name : ["com.qzone","com.tencent.mobileqq","com.tencent.tim"]
         */

        private String schema;
        private List<String> package_name;

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public List<String> getPackage_name() {
            return package_name;
        }

        public void setPackage_name(List<String> package_name) {
            this.package_name = package_name;
        }
    }
}
