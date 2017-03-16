package com.frowhy.wake.model;

import java.util.List;

/**
 * Wake
 * Created by frowhy on 2017/3/14.
 */

public class Scheme {

    private List<SchemesBean> schemes;

    public List<SchemesBean> getSchemes() {
        return schemes;
    }

    public void setSchemes(List<SchemesBean> schemes) {
        this.schemes = schemes;
    }

    public static class SchemesBean {
        /**
         * scheme : mqq
         * package_name : ["com.qzone","com.tencent.mobileqq","com.tencent.tim"]
         */

        private String scheme;
        private List<String> package_name;

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public List<String> getPackage_name() {
            return package_name;
        }

        public void setPackage_name(List<String> package_name) {
            this.package_name = package_name;
        }
    }
}
