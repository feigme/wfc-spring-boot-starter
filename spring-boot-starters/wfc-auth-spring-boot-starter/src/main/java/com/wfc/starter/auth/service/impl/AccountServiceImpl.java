package com.wfc.starter.auth.service.impl;

import com.wfc.starter.auth.constant.AccountEnableEnum;
import com.wfc.starter.auth.dal.entity.WfcAccountDO;
import com.wfc.starter.auth.dal.mapper.WfcAccountMapper;
import com.wfc.starter.auth.exception.WfcAuthException;
import com.wfc.starter.auth.jwt.JwtHandler;
import com.wfc.starter.auth.service.AccountService;
import com.wfc.starter.auth.web.cmd.PwdLoginCmd;
import com.wfc.starter.auth.web.cmd.PwdRegisterCmd;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author 飞影
 * @create by 2020-09-29 21:21
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private WfcAccountMapper wfcAccountMapper;
    @Autowired
    private JwtHandler jwtHandler;

    @Override
    public String loginByPwd(PwdLoginCmd pwdLoginCmd) {
        Validate.notNull(pwdLoginCmd, "登录参数为null");
        Validate.notBlank(pwdLoginCmd.getLoginName(), "登陆账号为空！");
        Validate.notBlank(pwdLoginCmd.getPassword(), "登陆密码为空！");

        WfcAccountDO accountDO = wfcAccountMapper.getAccountByLoginName(pwdLoginCmd.getLoginName());
        if (accountDO == null) {
            throw new WfcAuthException("当前用户不存在！");
        }

        if (AccountEnableEnum.DISABLE.getCode() == accountDO.getDisabled()) {
            throw new WfcAuthException("当前用户被锁定！");
        }

        String encyptPwd = DigestUtils.sha512Hex(DigestUtils.sha512Hex(pwdLoginCmd.getPassword()) + accountDO.getSlat());
        if (!StringUtils.equals(encyptPwd, accountDO.getPassword())) {
            throw new WfcAuthException("密码错误！");
        }

        return jwtHandler.createJWT(true, accountDO.getId(), accountDO.getLoginName());
    }

    @Override
    public WfcAccountDO loginByPhone(String phoneNum, String verifyCode) {
        Validate.notBlank(phoneNum, "手机号码为空！");
        Validate.notBlank(verifyCode, "验证码为空！");

        return null;
    }

    @Override
    public String registerByPwd(PwdRegisterCmd cmd) {
        Validate.notNull(cmd, "参数为null！");
        Validate.notBlank(cmd.getLoginName(), "登陆账号为空！");
        Validate.isTrue(cmd.getLoginName().length() >= 6 && cmd.getLoginName().length() <= 20, "账号长度6~20个字符！");
        Validate.notBlank(cmd.getPassword(), "登陆密码为空！");
        Validate.isTrue(cmd.getPassword().length() >= 6 && cmd.getPassword().length() <= 14, "密码长度6~14个字符！");
        Validate.notBlank(cmd.getConfirmPassword(), "确认密码为空！");
        Validate.isTrue(StringUtils.equals(cmd.getPassword(), cmd.getConfirmPassword()), "密码不一致！");

        WfcAccountDO accountDO = wfcAccountMapper.getAccountByLoginName(cmd.getLoginName());
        if (accountDO != null) {
            throw new WfcAuthException("账号已经存在！");
        }

        String slat = RandomStringUtils.randomAlphanumeric(6);
        String encyptPwd = DigestUtils.sha512Hex(DigestUtils.sha512Hex(cmd.getPassword()) + slat);

        accountDO = new WfcAccountDO();
        accountDO.setLoginName(cmd.getLoginName());
        accountDO.setPassword(encyptPwd);
        accountDO.setSlat(slat);
        accountDO.setDisabled(AccountEnableEnum.ENABLE.getCode());
        wfcAccountMapper.insert(accountDO);

        return jwtHandler.createJWT(true, accountDO.getId(), accountDO.getLoginName());
    }

    @Override
    public WfcAccountDO getAccountById(Long id) {
        return wfcAccountMapper.selectById(id);
    }
}