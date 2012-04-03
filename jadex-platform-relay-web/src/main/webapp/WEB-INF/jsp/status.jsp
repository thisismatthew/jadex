<jsp:include page="header.jsp" flush="true"/>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="jadex.base.relay.*" %>
<%@ page import="java.util.*" %>

<h2>Connected Platforms</h2>
<table>
	<tr>
		<th>&nbsp;</th>
		<th>Platform</th>
		<th>Host</th>
		<th>Location</th>
		<th>Connected Since</th>
		<th>Received Messages</th>
		<th>Avg. Transfer Rate</th>
	</tr>
	
	<%
		PlatformInfo[]	infos	= (PlatformInfo[])request.getAttribute("platforms");
		for(int i=0; i<infos.length; i++)
		{%>
			<tr class="<%= i%2==0 ? "even" : "odd" %>" title="<%= infos[i].toString() %>">
				<td>
					<% if(infos[i].getCountryCode()!=null) {%>
						<img src="<%= request.getContextPath() %>/resources/flags/flag-<%= infos[i].getCountryCode() %>.png"/>
					<% } %>
					<% if(infos[i].getScheme()!=null && infos[i].getScheme().endsWith("s")) {%>
						<img src="<%= request.getContextPath() %>/resources/lock.png"/>
					<% } %>
					</td>
				<td>
					<%= infos[i].getId() %> </td>
				<td>
					<%= infos[i].getHostName() %></td>
				<td>
					<%= infos[i].getLocation() %></td>
				<td>
					<%= infos[i].getConnectTime() %></td>
				<td class="number">
					<%= infos[i].getMessageCount() %> (<%= infos[i].getByteCount() %>)</td>
				<td class="number">
					<%= infos[i].getTransferRate() %></td>
			</tr>
	<%	} %>

</table>

<jsp:include page="footer.jsp" flush="true"/>
