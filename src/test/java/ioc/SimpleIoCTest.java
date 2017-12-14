package ioc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class SimpleIoCTest {
    @Mock
    private Config config;
    private SimpleIoC simpleIoC;

    @Before
    public void setUp() throws Exception {
        simpleIoC = new SimpleIoC(config);
    }

    @Test
    public void containerDefinitionsShouldBeEmpty() throws Exception {
        List<String> definitions = simpleIoC.getBeanDefinitions();

        then(definitions).isEmpty();
    }

    @Test
    public void beanDefinitionsWithOneBean() throws Exception {
        given(config.beanNames()).willReturn(singletonList("bean1"));

        then(simpleIoC.getBeanDefinitions()).containsExactly("bean1");
    }

    @Test
    public void beanDefinitionsWithSeveralBeans() throws Exception {
        String[] names = ThreadLocalRandom.current().ints().distinct().limit(10)
                .mapToObj(Integer::toString)
                .map("bean"::concat)
                .toArray(String[]::new);

        given(config.beanNames()).willReturn(Arrays.asList(names));

        then(simpleIoC.getBeanDefinitions()).containsExactly(names);
    }

    @Test
    public void beanNamesInConfigShouldBeUnique() throws Exception {
        given(config.beanNames()).willReturn(Arrays.asList("bean", "bean"));

        thenThrownBy(simpleIoC::getBeanDefinitions).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void getBeanCreatesInstanceOfDeclaredType() throws Exception {
        String name = "testBeanName";
        Class type = TestBean.class;

        BeanDefinition definition = mock(BeanDefinition.class);
        given(definition.getBeanName()).willReturn(name);
        given(definition.getBeanType()).willReturn(type);
        given(config.getBeanDefinition(name)).willReturn(definition);


        then(simpleIoC.getBean(name))
                .isNotNull()
                .isInstanceOf(TestBean.class);
    }

    @Test
    public void getBeanCreatesSingleton() throws Exception {
        String name = "testBeanName";
        Class type = TestBean.class;

        BeanDefinition definition = mock(BeanDefinition.class);
        given(definition.getBeanName()).willReturn(name);
        given(definition.getBeanType()).willReturn(type);
        given(config.getBeanDefinition(name)).willReturn(definition);

        Object existing = simpleIoC.getBean(name);
        then(simpleIoC.getBean(name)).isSameAs(existing);
    }

    @Test
    public void getBeanValidatesType() throws Exception {
        String name = "testBeanName";
        Class type = TestBean.class;
        Class differentType = String.class;

        BeanDefinition definition = mock(BeanDefinition.class);
        given(definition.getBeanName()).willReturn(name);
        given(definition.getBeanType()).willReturn(type);
        given(config.getBeanDefinition(name)).willReturn(definition);

        simpleIoC.getBean(name);

        given(definition.getBeanType()).willReturn(differentType);

        thenThrownBy(() -> simpleIoC.getBean(name)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getBeanWithClassCastsToType() throws Exception {
        String name = "testBeanName";
        Class<TestBean> type = TestBean.class;

        BeanDefinition definition = mock(BeanDefinition.class);
        given(definition.getBeanName()).willReturn(name);
        given(definition.getBeanType()).willReturn(type);
        given(config.getBeanDefinition(name)).willReturn(definition);


        TestBean bean = simpleIoC.getBean(name, type);
        then(bean).isNotNull();
    }

    @Test
    public void getBeanWithDependency() throws Exception {
        String dependencyName = "testBean";
        Class<TestBean> dependencyType = TestBean.class;
        BeanDefinition dependencyDefinition = mock(BeanDefinition.class);
        given(dependencyDefinition.getBeanName()).willReturn(dependencyName);
        given(dependencyDefinition.getBeanType()).willReturn(dependencyType);
        given(config.getBeanDefinition(dependencyName)).willReturn(dependencyDefinition);

        String beanName = "testBeanWithDependencies";
        Class<TestBeanWithDependencies> beanType = TestBeanWithDependencies.class;
        BeanDefinition definition = mock(BeanDefinition.class);
        given(definition.getBeanName()).willReturn(beanName);
        given(definition.getBeanType()).willReturn(beanType);
        given(config.getBeanDefinition(beanName)).willReturn(definition);

        Object bean = simpleIoC.getBean(beanName);
        then(bean).isInstanceOf(beanType);
        List<Object> dependencies = ((TestBeanWithDependencies) bean).dependencies;
        then(dependencies)
                .hasSize(1)
                .first().isInstanceOf(dependencyType);
    }

    static class TestBean {
    }

    static class TestBeanWithDependencies {
        private List<Object> dependencies;

        public TestBeanWithDependencies(TestBean dependency) {
            dependencies = singletonList(dependency);
        }
    }
}