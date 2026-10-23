package cc.coopersoft.keycloak.phone.providers.sender;

import cc.coopersoft.common.OptionalUtils;
import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.exception.MessageSendException;
import cc.coopersoft.keycloak.phone.providers.spi.MessageSenderService;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.RealmModel;

import java.util.Optional;

public class AliyunSmsSenderServiceProvider implements MessageSenderService {

    private static final Logger logger = Logger.getLogger(AliyunSmsSenderServiceProvider.class);

    private final Config.Scope config;
    private final RealmModel realm;

    private static final String ATTRIBUTE_PREFIX = "aliyun-sms-";

    public AliyunSmsSenderServiceProvider(Config.Scope config, RealmModel realm) {
        this.config = config;
        this.realm = realm;

    }

    @Override
    public void sendSmsMessage(TokenCodeType type, String phoneNumber, String code, int expires,
                               String kind) throws MessageSendException {
        String kindName = OptionalUtils.ofBlank(kind).orElse(type.name().toLowerCase());

        String region = Optional.ofNullable(config.get("region"))
                .orElse(realm.getAttribute(ATTRIBUTE_PREFIX + "region"));
        String key = Optional.ofNullable(config.get("key"))
                .orElse(realm.getAttribute(ATTRIBUTE_PREFIX + "key"));
        String secret = Optional.ofNullable(config.get("secret"))
                .orElse(realm.getAttribute(ATTRIBUTE_PREFIX + "secret"));
        DefaultProfile profile = DefaultProfile.getProfile(region, key, secret);
        IAcsClient client = new DefaultAcsClient(profile);
        String templateCode = Optional.ofNullable(
                        config.get(realm.getName().toLowerCase() + "-" + kindName + "-template"))
                .orElse(realm.getAttribute(ATTRIBUTE_PREFIX + kindName + "-template"));
        String signName = Optional.ofNullable(config.get("sign"))
                .orElse(realm.getAttribute(ATTRIBUTE_PREFIX + "sign"));

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", region);
        request.putQueryParameter("PhoneNumbers", phoneNumber);
        request.putQueryParameter("SignName", signName);
        request.putQueryParameter("TemplateCode", templateCode);

        request.putQueryParameter("TemplateParam",
                String.format("{\"code\":\"%s\",\"expires\":\"%s\"}", code, expires / 60));
        try {
            CommonResponse response = client.getCommonResponse(request);
            logger.debug(response.getData());
        } catch (ClientException e) {
            throw new MessageSendException(500, e.getErrCode(), e.getMessage());
        }
    }

    @Override
    public void close() {

    }

}
