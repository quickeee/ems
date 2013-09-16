[#ftl/]
[@b.head/]
[@b.grid items=businessLogs var="log" refresh="5"]
  [@b.gridbar]
    bar.addItem("导出",action.exportData("id:日志编号,operator:操作人员,operatorAt:操作时间,entry:入口,operation:日志内容/操作"));
  [/@]
  [@b.row]
    [@b.boxcol/]
    [@b.col title="时间" width="17%"  property="operateAt" ]${log.operateAt?string("yyyy-MM-dd HH:mm:ss")}[/@]
    [@b.col title="ip"  width="10%" property="ip"/]
    [@b.col title="资源"  width="10%" property="resource"/]
    [@b.col title="业务事件" width="60%" style="text-align:left" property="operation"]<span title="入口地址:${log.entry!} 客户端:${log.agent!}">&nbsp;${log.operator} ${log.operation}</span>[/@]
  [/@]
[/@]

[@b.foot/]