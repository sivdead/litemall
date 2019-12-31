package org.linlinjava.litemall.db.util;

import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author 无垠
 * @since 0.1.0
 */
@SuppressWarnings({"unchecked"})
public class DtoUtil extends BeanUtils {

    private static Logger logger = LoggerFactory.getLogger(DtoUtil.class);

    public static <S, T> PageInfo<T> copyPage(PageInfo<S> pageInfo, Class<T> clazz) {
        return copyPage(pageInfo, clazz, null, new String[]{});
    }

    public static <S, T> PageInfo<T> copyPage(PageInfo<S> pageInfo, Class<T> clazz, String[] ignoreProperties) {
        return copyPage(pageInfo, clazz, null, ignoreProperties);
    }

    public static <S, T> PageInfo<T> copyPage(PageInfo<S> pageInfo, Class<T> clazz, Consumer<T> consumer, String[] ignoreProperties) {
        PageInfo<T> resultPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, resultPageInfo, "list");
        List<T> targetList = copyList(pageInfo.getList(), clazz, consumer, ignoreProperties);
        resultPageInfo.setList(targetList);
        return resultPageInfo;
    }

    public static <S, T> List<T> copyList(List<S> sourceList, Class<T> clazz) {
        return copyList(sourceList, clazz, null, new String[]{});
    }

    public static <S, T> List<T> copyList(List<S> sourceList, Class<T> clazz, String[] ignoreProperties) {
        return copyList(sourceList, clazz, null, ignoreProperties);
    }


    public static <S, T> List<T> copyList(List<S> sourceList, Class<T> clazz, Consumer<T> consumer, String... ignoreProperties) {
        if(sourceList == null){
            return null;
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S sourceObject : sourceList) {
            targetList.add(copy(sourceObject, clazz, consumer, ignoreProperties));
        }
        return targetList;
    }

    public static <S, T> T copy(S eo, Class<T> clazz) {
        return copy(eo, clazz, new String[]{});
    }

    public static <S, T> T copy(S eo, Class<T> clazz, String[] ignoreProperties) {
        return copy(eo, clazz, null, ignoreProperties);
    }


    public static <S, T> T copy(S eo, Class<T> clazz, Consumer<T> consumer, String[] ignoreProperties) {
        T targetObj = null;
        try {
            targetObj = clazz.newInstance();
        } catch (Exception e) {
            logger.error(clazz.getName() + " create new instance error", e);
        }
        //如果创建实例失败会报NPE
        Assert.notNull(eo, "Source must not be null");
        Assert.notNull(targetObj, "Target must not be null");
        List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);
        Class<?> actualEditable = targetObj.getClass();
        PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
        for (PropertyDescriptor targetPd : targetPds) {
            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(eo.getClass(), targetPd.getName());
                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null &&
                            ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                        try {
                            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                readMethod.setAccessible(true);
                            }
                            Object value = readMethod.invoke(eo);
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            writeMethod.invoke(targetObj, value);
                        } catch (Throwable ex) {
                            throw new FatalBeanException(
                                    "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
                        }
                    }
                }
            }
        }
        //处理基础中台extField
        PropertyDescriptor sourceExtFieldPd = getPropertyDescriptor(eo.getClass(), "extFields");
        if (null != sourceExtFieldPd) {
            try {
                Map<String, Object> extFieldMap = (Map<String, Object>) sourceExtFieldPd.getReadMethod().invoke(eo);
                if (extFieldMap.size() > 0) {
                    Iterator<Map.Entry<String, Object>> iterator = extFieldMap.entrySet().iterator();
                    Map.Entry<String, Object> entry = null;
                    PropertyDescriptor targetFieldPd = null;
                    while (iterator.hasNext()) {
                        entry = iterator.next();
                        targetFieldPd = getPropertyDescriptor(clazz, entry.getKey());
                        if (null == targetFieldPd) {
                            continue;
                        }
                        targetFieldPd.getWriteMethod().invoke(targetObj, entry.getValue());
                    }
                }
            } catch (Exception e) {
                throw new FatalBeanException(
                        "Could not copy property '" + clazz.getName() + "' from source to target", e);
            }
        }
        if (consumer != null) {
            consumer.accept(targetObj);
        }
        return targetObj;
    }
}
