package io.jpress.web.admin;

import com.jfinal.core.Action;
import com.jfinal.core.JFinal;
import com.jfinal.kit.Ret;
import io.jboot.web.controller.annotation.RequestMapping;
import io.jpress.admin.menu.AdminMenuGroup;
import io.jpress.admin.menu.AdminMenuItem;
import io.jpress.admin.menu.AdminMenuManager;
import io.jpress.model.Permission;
import io.jpress.admin.permission.annotation.AdminPermission;
import io.jpress.service.PermissionService;
import io.jpress.service.Services;
import io.jpress.web.JPressAdminControllerBase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 * @Title: 首页
 * @Package io.jpress.web.admin
 */
@RequestMapping("/admin/permission")
public class AdminPermissionController extends JPressAdminControllerBase {

    private PermissionService permissionService = Services.get(PermissionService.class);

    /**
     * 同步所有可以进行控制的 Action 到数据库
     */
    public void syncActionPermissions() {
        List<Permission> adminPermissions = buildActionPermissions();
        int syncCount = permissionService.sync(adminPermissions);

        if (syncCount == 0) {
            renderJson(Ret.ok("msg", "权限已经是最新状态，无需更新"));
        } else {
            renderJson(Ret.ok("msg", "权限更新成功，共更新权限数 : " + syncCount));
        }
    }

    /**
     * 同步所有可以进行控制的 菜单 到数据库
     */
    public void syncMenuPermissions() {
        List<Permission> adminPermissions = buildMenuPermissions();
        int syncCount = permissionService.sync(adminPermissions);

        if (syncCount == 0) {
            renderJson(Ret.ok("msg", "权限已经是最新状态，无需更新"));
        } else {
            renderJson(Ret.ok("msg", "权限更新成功，共更新权限数 : " + syncCount));
        }
    }

    private List<Permission> buildMenuPermissions() {

        List<AdminMenuGroup> adminMenuGroups = new ArrayList<>();
        adminMenuGroups.addAll(AdminMenuManager.me().getSystemMenus());
        adminMenuGroups.addAll(AdminMenuManager.me().getModuleMenus());

        List<Permission> permissions = new ArrayList<>();
        for (AdminMenuGroup menuGroup : adminMenuGroups) {
            Permission groupPermission = new Permission();
            groupPermission.setType(Permission.TYPE_MENU);
            groupPermission.setText(menuGroup.getText());
            groupPermission.setNode(menuGroup.getId());
            groupPermission.setActionKey(menuGroup.getId());
            permissions.add(groupPermission);

            if (menuGroup.getItems() == null) {
                continue;
            }

            for (AdminMenuItem item : menuGroup.getItems()) {
                Permission itemPermission = new Permission();
                itemPermission.setType(Permission.TYPE_MENU);
                itemPermission.setText(item.getText());
                itemPermission.setNode(item.getGroupId());
                itemPermission.setActionKey(item.getGroupId() + ":" + item.getUrl());
                permissions.add(itemPermission);
            }
        }

        return permissions;
    }


    // 用于排除掉 BaseController 中的几个成为了 action 的方法
    private static Set<String> excludedMethodName = buildExcludedMethodName();

    private static Set<String> buildExcludedMethodName() {
        Set<String> excludedMethodName = new HashSet<String>();
        Method[] methods = JPressAdminControllerBase.class.getMethods();
        for (Method m : methods) {
            excludedMethodName.add(m.getName());
        }
        return excludedMethodName;
    }

    private static List<Permission> buildActionPermissions() {
        List<Permission> permissions = new ArrayList<>();
        List<String> allActionKeys = JFinal.me().getAllActionKeys();

        String[] urlPara = new String[1];
        for (String actionKey : allActionKeys) {
            // 只处理后台的权限 和 API的权限
            if (actionKey.startsWith("/admin") || actionKey.startsWith("/api")) {

                Action action = JFinal.me().getAction(actionKey, urlPara);
                if (action == null || excludedMethodName.contains(action.getMethodName())) {
                    continue;
                }

                AdminPermission permissionAnnotation = action.getMethod().getAnnotation(AdminPermission.class);
                String text = permissionAnnotation == null ? null : permissionAnnotation.value();
                String controller = action.getControllerClass().getName();

                Permission permission = new Permission();
                permission.setActionKey(actionKey);
                permission.setNode(controller);
                permission.setText(text);
                permission.setType(Permission.TYPE_ACTION);

                permissions.add(permission);
            }
        }

        return permissions;
    }

}
