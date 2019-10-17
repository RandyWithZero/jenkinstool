package com.tongtech.cmp.jenkinstool.jobxml.exception;

/**
 *
 *description 
 *
 *version 0.1
 *createDate 2019/10/16 19:00
 *updateDate 2019/10/16 19:00
 *@author wangshaoqi
 */
public class EmptyJenkinsPluginsInfoException extends Exception{
    private final static String MESSAGE="Jenkins插件信息容器没有导入插件信息，为空！";
    public EmptyJenkinsPluginsInfoException(){
        super(MESSAGE);
    }
}
