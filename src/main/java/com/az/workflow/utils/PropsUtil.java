package com.az.workflow.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.util.Properties;

/**
 * 属性文件工具类
 */
public final class PropsUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropsUtil.class);

    public static Properties loadProps(String fileName) {
        Properties props = new Properties();
        InputStream is = null;
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            is = resource.getInputStream();
            //解决中文乱码
            BufferedReader bf = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            if (null == is) {
                throw new FileNotFoundException(fileName + " file is not found!");
            }
            props = new Properties();
            props.load(bf);
        } catch (IOException e) {
            logger.error("load properties file failure", e);
        } finally {
            try {
                if(is!=null)
                    is.close();
            } catch (IOException e) {
                logger.error("close input stream failure", e);
            }
        }
        return props;
    }

    /*
     * 获取字符型属性（默认值为空字符串）
     */
    public static String getString(Properties props, String key) {
        return getString(props, key, "");
    }

    /*
     * 获取字符型属性（可指定默认值）
     */
    public static String getString(Properties props, String key, String defaultValue) {
        String value = defaultValue;
        if (props.containsKey(key)) {
            value = props.getProperty(key);
        }
        return value;
    }

    /*
     * 获取数值型属性（默认值为0）
     */
    public static int getInt(Properties props, String key) {
        return getInt(props, key, 0);
    }

    /*
     * 获取数值型属性（可指定默认值）
     */
    public static int getInt(Properties props, String key, int defaultValue) {
        int value = defaultValue;
        if (props.containsKey(key)) {
            value = Integer.valueOf(props.getProperty(key));
        }
        return value;
    }

    /*
     * 获取布尔型属性（默认为false）
     */
    public static boolean getBoolean(Properties props, String key) {
        return getBoolean(props, key, false);
    }

    /*
     * 获取布尔型属性（可指定默认值）
     */
    public static boolean getBoolean(Properties props, String key, Boolean defaultValue) {
        boolean value = defaultValue;
        if (props.containsKey(key)) {
            value = Boolean.valueOf(props.getProperty(key));
        }
        return value;
    }
}
