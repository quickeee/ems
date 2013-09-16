[#ftl]
[@b.head/]
[#include "nav.ftl"/]
[@b.toolbar title="系统资源访问记录"/]
[@b.grid items=requests var="r" refresh="10" emptyMsg="当前服务器没有访问请求."]
  [@b.gridbar]
  bar.addPrint("${b.text("action.print")}");
  [/@]
  [@b.row]
    [@b.col width="5%" title="序号"]${r_index+1}[/@]
    [@b.col width="33%" title="资源" align="left" property="uri"/]
    [@b.col width="25%" title="参数" align="left" property="params"/]
    [@b.col width="12%" title="帐号" property="username"][#if r.username??][@b.a target="_blank" href="/security/user!info?name=${r.username}"]${(r.username)}[/@][/#if][/@]
    [@b.col width="15%" title="开始" property="beginAt" ][#assign beginAt=beginAts[r.sessionid]/]${beginAt?string("yyyy-MM-dd HH:mm:ss")}[/@]
    [@b.col width="10%" title="持续时间(ms)" property="duration" sort="endAt-beginAt"/]
  [/@]
[/@]
[@b.foot/]