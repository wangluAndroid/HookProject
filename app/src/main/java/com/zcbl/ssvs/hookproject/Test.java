package com.zcbl.ssvs.hookproject;

import java.lang.reflect.Field;

/**
 * Created by serenitynanian on 2018/6/1.
 *
 *
 */

public class Test {

    private static final Person person  = new Person("android",22);


    public static void main(String[] args) {


        try {
            Class<?> aClass = Class.forName("com.zcbl.ssvs.hookproject.Test");
            Field person = aClass.getDeclaredField("person");
            person.setAccessible(true);
            Object object = person.get(null);

            Class<?> aClass1 = object.getClass();
            Field name = aClass1.getDeclaredField("name");
            name.setAccessible(true);
            Object o = name.get(object);
            System.out.println("----------------->"+o.toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


    }

}
