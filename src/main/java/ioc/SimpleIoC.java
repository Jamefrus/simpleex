package ioc;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleIoC {
    private Config config;
    private Map<String, Object> pool;

    public SimpleIoC(Config config) {
        this.config = config;
        pool = new HashMap<>();
    }

    public List<String> getBeanDefinitions() {
        List<String> names = config.beanNames();
        if (areUnique(names)) {
            return names;
        } else {
            throw new IllegalArgumentException("Bean definitions are not unique");
        }
    }

    private boolean areUnique(List<String> names) {
        long uniqueDefinitionCount = names.stream().distinct().count();
        int allDefinitionCount = names.size();
        return uniqueDefinitionCount == allDefinitionCount;
    }

    public Object getBean(String name) {
        Class beanType = config.getBeanDefinition(name).getBeanType();
        try {
            return createOrFindInstance(name, beanType);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Object createOrFindInstance(String beanName, Class beanType) throws ReflectiveOperationException {
        Object instance = findInstance(beanName, beanType);
        return instance != null ? instance : createInstance(beanType);
    }

    private Object findInstance(String beanName, Class beanType) throws ReflectiveOperationException {
        Object bean = pool.get(beanName);
        if (bean == null) {
            return createAndSaveInstance(beanName, beanType);
        } else if (beanType.isInstance(bean)) {
            return bean;
        } else {
            throw new IllegalStateException("Invalid bean type");
        }
    }

    private Object createAndSaveInstance(String beanName, Class beanType) throws ReflectiveOperationException {
        Object newInstance = createInstance(beanType);
        pool.put(beanName, newInstance);
        return newInstance;
    }

    private Object createInstance(Class<?> beanType) throws ReflectiveOperationException {
        Constructor<?> constructor = findConstructor(beanType);
        constructor.setAccessible(true);
        Object[] parameters = matchParameters(constructor);
        return constructor.newInstance(parameters);
    }

    private Object[] matchParameters(Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameterTypes())
                .map(Class::getSimpleName)
                .map(this::decapitalize)
                .map(this::getBean)
                .toArray();
    }

    private String decapitalize(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private Constructor<?> findConstructor(Class<?> beanType) throws NoSuchMethodException {
        Constructor<?>[] constructors = beanType.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalStateException("A lot of constructors");
        }
        return constructors[0];
    }

    public <T> T getBean(String name, Class<T> type) throws ReflectiveOperationException {
        Object bean = getBean(name);
        return type.cast(bean);
    }
}
