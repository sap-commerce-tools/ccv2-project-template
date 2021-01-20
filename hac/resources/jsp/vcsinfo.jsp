<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<% pageContext.setAttribute("newLineChar", "\n"); %>
<html>
<head>
	<title>VCS Info</title>
	<link rel="stylesheet" href="<c:url value="/static/css/table.css"/>" type="text/css" media="screen, projection"/>
	<script type="text/javascript" src="<c:url value="/static/js/jquery.dataTables.min.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/static/js/history.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/static/js/vcsinfo.js"/>"></script>
</head>
<body>
<div class="prepend-top span-17 colborder" id="content">
	<button id="toggleSidebarButton">&gt;</button>
	<div class="marginLeft marginBottom" id="inner">
		<h2>VCS Info</h2>
		<table id="vcsinfo">
			<thead>
			<tr>
				<th width="25%">Property</th>
				<th>Value</th>
			</tr>
			</thead>
			<tbody>
			<c:forEach items="${items}" var="item">
				<tr>
					<td width="25%">${item.key}</td>
					<td>${item.value}</td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</div>
</div>
<div id="dialogContainer"></div>
</body>
</html>

