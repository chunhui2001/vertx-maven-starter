<#include "header.ftl">
<div class="row">

  <div class="col-md-12 mt-1" style="margin-top: 1em !important;">

    <div class="float-xs-right">
      <form class="form-inline" action="/create" method="post">

        <div class="form-group">
          <input type="text" class="form-control" id="name" name="name" placeholder="New page name">
        </div>

          <button type="submit" class="btn btn-primary" style="margin-left: .8em;">Create</button>
      </form>
    </div>

    <h2 class="display-4" style="margin-top: .6em;font-size: 2em;">${context.title}</h2>
  </div>

  <div class="col-md-12 mt-1">

    <#list context.pages>
      <h2 style="font-size: 1.5em;
    font-style: italic;
    color: gray;">Pages:</h2>
      <ul>
        <#items as page>
        <li><a href="/wiki/${page[0]}">${page[1]}, </a>
          <br />
          <span style="color: goldenrod;">${page[2]}</span></li>
        </#items>
      </ul>
      <#else>
      <p style="color: darkorchid">The wiki is currently empty!</p>
    </#list>

      <a ="submit" href="/backup" class="btn btn-info">Gist Backup</a>

    <div style="padding: 1em;
    margin-top: 1em;
    background-color: burlywood;">
      <p style="margin-bottom: .3em;color: brown;">Successfully creatd a backup:</p>
      <a href="https://gist.github.com/58abd4dc8636685140b4ec2e174295f1 " target="_blank">
        https://gist.github.com/58abd4dc8636685140b4ec2e174295f1
      </a>
    </div>

  </div>

  </div>

<#include "footer.ftl">
