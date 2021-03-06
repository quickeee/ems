/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2016, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.ems.security.web.action.session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.beangle.commons.bean.comparators.PropertyComparator;
import org.beangle.commons.dao.query.builder.OqlBuilder;
import org.beangle.commons.lang.Strings;
import org.beangle.ems.web.action.SecurityActionSupport;
import org.beangle.security.blueprint.session.model.SessioninfoLogBean;

/**
 * 用户登陆退出的会话管理
 * 
 * @author chaostone
 */
public class LogAction extends SecurityActionSupport {

  /**
   * 显示用户某时间段的登陆记录
   */
  public String search() {
    OqlBuilder<SessioninfoLogBean> query = OqlBuilder.from(SessioninfoLogBean.class, "sessioninfoLog");
    addConditions(query);
    String orderBy = get("orderBy");
    if (null == orderBy) orderBy = "sessioninfoLog.loginAt desc";
    query.limit(getPageLimit()).orderBy(orderBy);
    put("sessioninfoLogs", entityDao.search(query));
    return forward();
  }

  private void addTimeCondition(OqlBuilder<SessioninfoLogBean> query) {
    String stime = get("startTime");
    String etime = get("endTime");
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date sdate = null, edate = null;
    try {
      if (Strings.isNotBlank(stime)) {
        sdate = df.parse(stime);
      }
      if (Strings.isNotBlank(etime)) {
        // 截至日期增加一秒
        edate = df.parse(etime);
        Calendar gc = new GregorianCalendar();
        gc.setTime(edate);
        gc.roll(Calendar.SECOND, 1);
        edate = gc.getTime();
      }
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    if (null != sdate && null == edate) {
      query.where("sessioninfoLog.loginAt >=:sdate", sdate);
    } else if (null != sdate && null != edate) {
      query.where("sessioninfoLog.loginAt >=:sdate and sessioninfoLog.loginAt <:edate", sdate, edate);
    } else if (null == sdate && null != edate) {
      query.where("sessioninfoLog.loginAt <:edate", edate);
    }
  }

  /**
   * 显示登陆次数
   */
  public String loginCountStat() {
    OqlBuilder<SessioninfoLogBean> query = OqlBuilder.from(SessioninfoLogBean.class, "sessioninfoLog");
    addConditions(query);
    query.select("sessioninfoLog.username,sessioninfoLog.fullname,count(*)")
        .groupBy("sessioninfoLog.username,sessioninfoLog.fullname").orderBy(get("orderBy"))
        .limit(getPageLimit());
    put("loginCountStats", entityDao.search(query));
    return forward();
  }

  private void addConditions(OqlBuilder<SessioninfoLogBean> query) {
    populateConditions(query);
    addTimeCondition(query);
  }

  /**
   * 显示角色某时间段的登陆记录
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public String timeIntervalStat() {
    OqlBuilder<SessioninfoLogBean> query = OqlBuilder.from(SessioninfoLogBean.class, "sessioninfoLog");
    addConditions(query);
    query.select("hour(sessioninfoLog.loginAt),count(*)").groupBy("hour(sessioninfoLog.loginAt)");
    List rs = entityDao.search(query);
    Collections.sort(rs, new PropertyComparator("[0]"));
    put("logonStats", rs);
    return forward();
  }
}
