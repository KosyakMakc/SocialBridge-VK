package io.github.kosyakmakc.socialBridgeVk.DatabaseTables;

import java.time.Instant;
import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import io.github.kosyakmakc.socialBridge.DatabasePlatform.Tables.IDatabaseTable;

@DatabaseTable(tableName = VkUserTable.TABLE_NAME)
public class VkUserTable implements IDatabaseTable {
    public static final String TABLE_NAME = "vk_user";

    public static final String ID_FIELD_NAME = "id";
    public static final String USERNAME_FIELD_NAME = "username";
    public static final String FIRST_NAME_FIELD_NAME = "first_name";
    public static final String LAST_NAME_FIELD_NAME = "last_name";
    public static final String LOCALIZATION_FIELD_NAME = "localization";
    public static final String CREATED_AT_FIELD_NAME = "created_at";
    public static final String UPDATED_AT_FIELD_NAME = "updated_at";

    @DatabaseField(columnName = ID_FIELD_NAME, id = true)
    private long id;

    @DatabaseField(columnName = USERNAME_FIELD_NAME, canBeNull = true)
    private String username;

    @DatabaseField(columnName = FIRST_NAME_FIELD_NAME)
    private String firstName;

    @DatabaseField(columnName = LAST_NAME_FIELD_NAME, canBeNull = true)
    private String lastName;

    @DatabaseField(columnName = LOCALIZATION_FIELD_NAME, canBeNull = true)
    private String localization;

    @DatabaseField(columnName = CREATED_AT_FIELD_NAME)
    private Date createdAt;

    @DatabaseField(columnName = UPDATED_AT_FIELD_NAME)
    private Date updatedAt;

    public VkUserTable() {

    }

    public VkUserTable(
        long id,
        String username,
        String firstName,
        String lastName,
        String localization
    ) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.localization = localization;
        this.createdAt = Date.from(Instant.now());
        this.updatedAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLocalization() {
        return localization;
    }

    public void setLocalization(String localization) {
        this.localization = localization;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
