package com.zzy.mywebsitebackend.shiro;

import com.zzy.mywebsitebackend.Data.Entity.BackendUser;
import com.zzy.mywebsitebackend.Data.Entity.PermissionEnum;
import com.zzy.mywebsitebackend.Service.BackendUserService;
import com.zzy.mywebsitebackend.Util.JwtUtil;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyRealm extends AuthorizingRealm {

    @Autowired
    private BackendUserService backendUserService;

    /**
     * 大坑！，必须重写此方法，不然Shiro会报错
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 只有当需要检测用户权限的时候才会调用此方法，例如checkRole,checkPermission之类的
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = JwtUtil.getClaim(principals.toString(),"username");
        BackendUser user = backendUserService.selectByUserName(username);
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        simpleAuthorizationInfo.addRole(user.getRole());
        for (PermissionEnum permissionEnum : user.getPermissionWithSet()) {
            simpleAuthorizationInfo.addStringPermission(permissionEnum.getName());
        }
        return simpleAuthorizationInfo;
    }

    /**
     * 默认使用此方法进行用户名正确与否验证，错误抛出异常即可。
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken auth) throws AuthenticationException {
        String token = (String) auth.getCredentials();
        // 解密获得username，用于和数据库进行对比
        String username = JwtUtil.getClaim(token,"username");
        if (username == null) {
            throw new AuthenticationException("token invalid");
        }

        BackendUser user = backendUserService.selectByUserName(username);
        if (user == null) {
            throw new AuthenticationException("User didn't existed!");
        }

        if (! JwtUtil.verify(token,user.getPassword())) {
            throw new AuthenticationException("Username or password error");
        }

        return new SimpleAuthenticationInfo(token, token, "my_realm");
    }
}
