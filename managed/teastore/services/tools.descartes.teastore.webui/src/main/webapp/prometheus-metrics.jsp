<%--https://github.com/javamelody/javamelody/wiki/UserGuideAdvanced#exposing-metrics-to-prometheus--%>

<%@ taglib uri="javamelody-prometheus" prefix="prometheus" %>
<%@ page session="false"%>
<%@ page contentType="text/plain; version=0.0.4;charset=UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>

<script src="https://code.jquery.com/jquery-3.6.4.min.js"></script>

<prometheus:mbean jmxValue="java.lang:type=OperatingSystem.SystemCpuLoad"
                  metricName="system_cpu_usage" metricType="gauge" />

<prometheus:request requestId="http9b051a0212745888c373b46ee5f27d9d6d905d45" />
<prometheus:standard includeLastValue="false"/>