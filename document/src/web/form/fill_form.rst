FormBean
------------------------------------
被称为表单，通常是指有多个请求参数被同时传递到后段应用进行处理，一个典型的场景就是填写注册信息。虽然只有一两个请求参数的也可以被称为表单，但是我们还是更愿意把众多参数一起递交到服务器的操作看做是表单。

我们先看一下一个有 5个递交参数的请求，使用 Hasor 已知的形式如何获取。

.. code-block:: java
    :linenos:

    @MappingTo("/helloAcrion.do")
    public class HelloAcrion extends WebController {
        public void execute(RenderInvoker invoker,
                            @ReqParam("param_1") String param_1,
                            @ReqParam("param_2") String param_2,
                            @ReqParam("param_3") String param_3,
                            @ReqParam("param_4") String param_4,
                            @ReqParam("param_5") String param_5){
            ...
        }
    }


通过 @Params 注解，Hasor 允许您更简单的方式获取这个表单数据。首先我们先定义一个表单 FormBean，然后通过 @Params 获取表单数据。如下：

.. code-block:: java
    :linenos:

    public class ParamsFormBean {
        @ReqParam("param_1")
        private String param_1;
        @ReqParam("param_2")
        private String param_2;
        @ReqParam("param_3")
        private String param_3;
        @ReqParam("param_4")
        private String param_4;
        @ReqParam("param_5")
        private String param_5;
        ...
    }

    @MappingTo("/helloAcrion.do")
    public class HelloAcrion extends WebController {
        public void execute(RenderInvoker invoker, @Params() ParamsFormBean formBean){
            ...
        }
    }


