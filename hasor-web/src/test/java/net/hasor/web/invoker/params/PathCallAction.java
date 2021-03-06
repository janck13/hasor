package net.hasor.web.invoker.params;
import net.hasor.web.annotation.MappingTo;
import net.hasor.web.annotation.PathParameter;
import net.hasor.web.annotation.Post;
import net.hasor.web.invoker.beans.SelectEnum;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
//
@MappingTo("/{byteParam}/{floatParam}/path_param.do")
public class PathCallAction {
    //
    @Post
    public Map<String, Object> execute(//
            @PathParameter("byteParam") byte byteParam, @PathParameter("shortParam") short shortParam,  //
            @PathParameter("intParam") int intParam, @PathParameter("longParam") long longParam,    //
            @PathParameter("floatParam") float floatParam, @PathParameter("doubleParam") double doubleParam,//
            @PathParameter("charParam") char charParam, @PathParameter("strParam") String strParam, //
            @PathParameter("enumParam") SelectEnum enumParam,//
            @PathParameter("bigInteger") BigInteger bigInteger, @PathParameter("bigDecimal") BigDecimal bigDecimal,//
            //
            @PathParameter("urlParam") URL urlParam, @PathParameter("uriParam") URI uriParam, @PathParameter("fileParam") File fileParam,
            //
            //
            @PathParameter("utilData") java.util.Date utilData, @PathParameter("utilCalendar") java.util.Calendar utilCalendar, //
            @PathParameter("sqlData") java.sql.Date sqlData, @PathParameter("sqlTime") java.sql.Time sqlTime, @PathParameter("sqlTimestamp") java.sql.Timestamp sqlTimestamp //
    ) {
        Map<String, Object> dataMap = new HashMap<>();
        //
        dataMap.put("byteParam", byteParam);
        dataMap.put("shortParam", shortParam);
        dataMap.put("intParam", intParam);
        dataMap.put("longParam", longParam);
        dataMap.put("floatParam", floatParam);
        dataMap.put("doubleParam", doubleParam);
        dataMap.put("charParam", charParam);
        dataMap.put("strParam", strParam);
        //
        dataMap.put("enumParam", enumParam);
        dataMap.put("bigInteger", bigInteger);
        dataMap.put("bigDecimal", bigDecimal);
        dataMap.put("urlParam", urlParam);
        dataMap.put("uriParam", uriParam);
        dataMap.put("fileParam", fileParam);
        //
        dataMap.put("utilData", utilData);
        dataMap.put("utilCalendar", utilCalendar);
        dataMap.put("sqlData", sqlData);
        dataMap.put("sqlTime", sqlTime);
        dataMap.put("sqlTimestamp", sqlTimestamp);
        return dataMap;
    }
}
