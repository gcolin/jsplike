<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<fmt:setBundle basename="message" />
<%-- var alias = REQUEST_ATTRIBUTE as java.lang.String --%>
some text
<fmt:message key="the.message.key">
    <fmt:param value="Bruce Wayne"/>
    <fmt:param value="${alias}"/>
</fmt:message>
some text
