<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <meta http-equiv="x-ua-compatible" content="ie=edge">
  <link rel="stylesheet" href="/static/bootstrap-4.0.0/css/bootstrap.min.css">
  <title>${context.title} | Vert.x-powered</title>
</head>
<body>

<div style="background-color: black;color: darkgray;">
  <div class="container">
    <div class="row" style="text-align: right">
      <div class="col-md-12" style="padding: .3em 0;">

        <#if context.username == "anonymous user" >
          <a href="/login" style="color: whitesmoke;text-decoration: none;">${context.username}</a>
        </#if>

        <#if context.username != "anonymous user" >
          <a href="/logout" role="button" aria-pressed="true" style="color: whitesmoke;text-decoration: none;">${context.username}</a>
        </#if>

      </div>
    </div>
  </div>
</div>

<div class="container" style="margin-top: 1em;">
