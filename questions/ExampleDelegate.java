package com.yjnull.fastagripos.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.yjnull.latte_core.delegates.LatteDelegate;
import com.yjnull.latte_core.net.RestClient;
import com.yjnull.latte_core.net.callback.IError;
import com.yjnull.latte_core.net.callback.IFailure;
import com.yjnull.latte_core.net.callback.IRequest;
import com.yjnull.latte_core.net.callback.ISuccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by yangya on 2018/7/3.
 *
 */

public class ExampleDelegate extends LatteDelegate {
    @Override
    public Object setLayout() {
        return R.layout.delegate_example;
    }

    @Override
    public void onBindView(@Nullable Bundle savedInstanceState, View rootView) {
        //testRestClient();
    }

    public static void main(String[] args) throws NoSuchMethodException {
        Method method = ExampleDelegate.class.getDeclaredMethod("listRepos", String.class);
        Type returnType = method.getGenericReturnType();
        Annotation[] annotations = method.getAnnotations();
        System.out.println(returnType);
        System.out.println(returnType instanceof ParameterizedType);
        for (Annotation annotation : annotations) {
            System.out.println(annotation.annotationType());
            System.out.println(annotation);
        }

        Type type = returnType;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
            // suspects some pathological case related to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) throw new IllegalArgumentException();
            Class<?> clzz = (Class<?>) rawType;
            System.out.println(clzz.getName());
            System.out.println(clzz == Call.class);
        }

        System.out.println("-----------------------------------------");
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] types = parameterizedType.getActualTypeArguments();
        Type paramType = types[0];
        if (paramType instanceof WildcardType) {
            System.out.println( ((WildcardType) paramType).getUpperBounds()[0]);
        }
        System.out.println(paramType);
    }

    @GET("users/{user}/repos")
    public Call<List<Integer>> listRepos(@Path("user") String user) {
        return null;
    }
}
