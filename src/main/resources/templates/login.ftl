<#include "header.ftl">
<div class="row">
  <div class="col-md-12 mt-1">
    <form action="/login-auth" method="POST" class="form-horizontal" role="form">

      <#if context.error >
        <div class="form-group">
          <div class="alert alert-danger"  style="margin: 0 15px;" role="alert">
            登录失败: <br />
            用户名或密码错误!
          </div>
        </div>
      </#if>


      <div class="form-group">
        <label for="username" class="col-sm-2 control-label">名字</label>
        <div class="col-sm-10">
          <input type="text" class="form-control" id="username" name="username" placeholder="login">
        </div>
      </div>
      <div class="form-group">
        <label for="password" class="col-sm-2 control-label">姓</label>
        <div class="col-sm-10">
          <input type="password" class="form-control" id="password" name="password" placeholder="password">
        </div>
      </div>
      <div class="form-group">
        <div class="col-sm-offset-2 col-sm-10">
          <div class="checkbox">
            <label>
              <input type="checkbox">请记住我
            </label>
          </div>
        </div>
      </div>
      <div class="form-group">


        <div class="col-sm-offset-2 col-sm-10">
          <button type="submit" class="btn btn-primary">登录</button>
        </div>
      </div>
    </form>
  </div>
</div>
<#include "footer.ftl">
