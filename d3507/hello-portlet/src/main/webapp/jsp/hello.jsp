<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>

<portlet:defineObjects />
<%@ page import = "org.exoplatform.forum.common.user.CommonContact,
org.exoplatform.forum.common.user.ContactProvider,
org.exoplatform.container.PortalContainer"
%>
<%!
  public CommonContact getPersonalContact(String userId) {
		
		ContactProvider provider = (ContactProvider) PortalContainer.getComponent(ContactProvider.class);
		return provider.getCommonContact(userId);
	}
%>

<%
	String contextPath = request.getContextPath();
	CommonContact contact = getPersonalContact("root");
%>

<link rel="stylesheet" type="text/css" href="<%=contextPath%>/skin/Stylesheet.css"/>
<table class="ContactTable">
	<tr>
		<td>UserID</td><td>FullName</td><td>Email</td>
	</tr>
	<tr>
		<td><%= "root"%></td>
		<td><%= contact.getFullName()%></td>
		<td><%= contact.getEmailAddress()%></td>
	</tr>
</table>
