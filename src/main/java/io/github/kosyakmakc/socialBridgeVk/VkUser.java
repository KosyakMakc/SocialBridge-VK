package io.github.kosyakmakc.socialBridgeVk;
import io.github.kosyakmakc.socialBridge.ITransaction;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.IdentifierType;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;
import io.github.kosyakmakc.socialBridgeVk.DatabaseTables.VkUserTable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.vk.api.sdk.objects.users.UserFull;

public class VkUser extends SocialUser implements Comparable<VkUser> {
    private final VkUserTable userRecord;
    private final Identifier id;

    public VkUser(VkPlatform socialPlatform, VkUserTable userRecord) {
        super(socialPlatform);
        this.userRecord = userRecord;
        this.id = new Identifier(IdentifierType.Long, userRecord.getId());
    }

    @Override
    public String getName() {
        var lastname = userRecord.getLastName();
        if (lastname == null) {
            return userRecord.getFirstName();
        }
        else {
            return userRecord.getFirstName() + ' ' + userRecord.getLastName();
        }
    }

    @Override
    public CompletableFuture<Boolean> sendMessage(String message, HashMap<String, String> placeholders) {
        return ((VkPlatform) getPlatform()).sendMessage(this, message, placeholders);
    }

    @Override
    public CompletableFuture<Boolean> sendMessage(MessageKey message, String locale, HashMap<String, String> placeholders, ITransaction transaction) {
        return getPlatform()
            .getBridge()
            .getLocalizationService()
            .getMessage(locale, message, transaction)
            .thenCompose(messageTemplate -> sendMessage(messageTemplate, placeholders));
    }

    @Override
    public String getLocale() {
        var userLocale = userRecord.getLocalization();
        return userLocale == null ? LocalizationService.defaultLocale : userLocale;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public int compareTo(VkUser anotherUser) {
        var delta = (long) this.getId().value() - (long) anotherUser.getId().value();
        return delta > 0
            ? 1
            : delta < 0
                ? -1
                : 0;
    }

    public VkUserTable getUserRecord() {
        return userRecord;
    }

    boolean isNullOrBlank (String str) {
        return str == null || str.isBlank();
    }

    public boolean tryActualize(UserFull vkUser) {
        var changed = false;

        if (!isNullOrBlank(vkUser.getDomain())
         && !vkUser.getDomain().equals(userRecord.getUsername())) {
            userRecord.setUsername(vkUser.getDomain());
            changed = true;
        }
        if (!isNullOrBlank(vkUser.getFirstName())
         && !vkUser.getFirstName().equals(userRecord.getFirstName())) {
            userRecord.setFirstName(vkUser.getFirstName());
            changed = true;
        }
        if (!isNullOrBlank(vkUser.getLastName())
         && !vkUser.getLastName().equals(userRecord.getLastName())) {
            userRecord.setLastName(vkUser.getLastName());
            changed = true;
        }
        // if (!isNullOrBlank(vkUser.getLanguageCode())
        //  && !vkUser.getLanguageCode().equals(userRecord.getLocalization())) {
        //     userRecord.setLocalization(vkUser.getLanguageCode());
        //     changed = true;
        // }

        userRecord.setUpdatedAt(Date.from(Instant.now()));

        // non-blocking save user in background
        ((VkPlatform) getPlatform()).getBridge().doTransaction(transaction -> {
            var databaseContext = transaction.getDatabaseContext();

            var table = databaseContext.getDaoTable(VkUserTable.class);
            try {
                table.update(userRecord);
                return CompletableFuture.completedFuture(true);
            } catch (SQLException e) {
                e.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
        });

        return changed;
    }
}
