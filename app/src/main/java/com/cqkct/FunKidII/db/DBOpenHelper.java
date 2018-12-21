package com.cqkct.FunKidII.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DaoMaster;

import org.greenrobot.greendao.database.Database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DBOpenHelper extends DaoMaster.OpenHelper {
    private static final String TAG = DBOpenHelper.class.getSimpleName();

    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        L.i(TAG, "Upgrading schema from version " + oldVersion + " to " + newVersion);

        for (Migration migration : getMigrations()) {
            if (oldVersion < migration.getVersion()) {
                migration.runMigration(db);
            }
        }
    }

    private interface Migration {
        Integer getVersion();
        void runMigration(Database db);
    }

    private List<Migration> getMigrations() {
        List<Migration> migrations = new ArrayList<>();
        migrations.add(new MigrationV2());
        migrations.add(new MigrationV3());
        migrations.add(new MigrationV4());
        migrations.add(new MigrationV5());
        migrations.add(new MigrationV6());
        migrations.add(new MigrationV7());
        migrations.add(new MigrationV8());
        migrations.add(new MigrationV9());
        migrations.add(new MigrationV10());

        Comparator<Migration> migrationComparator = new Comparator<Migration>() {
            @Override
            public int compare(Migration m1, Migration m2) {
                return m1.getVersion().compareTo(m2.getVersion());
            }
        };
        Collections.sort(migrations, migrationComparator);

        return migrations;
    }

    private static class MigrationV2 implements Migration {

        @Override
        public Integer getVersion() {
            return 2;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN BATTERY_PERCENT INTEGER");
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN STEP_COUNT INTEGER");
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN SENSOR_DATA_TIME INTEGER");
        }
    }

    private static class MigrationV3 implements Migration {

        @Override
        public Integer getVersion() {
            return 3;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN BATTERY_VOLTAGE INTEGER");
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN BATTERY_LEVEL INTEGER");
            db.execSQL("ALTER TABLE SOS_ENTITY ADD COLUMN SYNCED INTEGER");
            db.execSQL("ALTER TABLE CLASS_DISABLE_ENTITY ADD COLUMN SYNCED INTEGER");
        }
    }

    private static class MigrationV4 implements Migration {

        @Override
        public Integer getVersion() {
            return 4;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("DROP TABLE IF EXISTS \"LOCATION_ENTITY\"");
            db.execSQL("CREATE TABLE \"LOCATION_ENTITY\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"DEVICE_ID\" TEXT NOT NULL ," + // 1: deviceId
                    "\"DATE\" TEXT NOT NULL ," + // 2: date
                    "\"LOCATIONS_DATA\" BLOB NOT NULL ," + // 3: locationsData
                    "\"COMPLETE\" INTEGER NOT NULL );"); // 4: complete
        }
    }

    private static class MigrationV5 implements Migration {

        @Override
        public Integer getVersion() {
            return 5;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("DROP TABLE IF EXISTS \"COLLECT_PRAISE_ENTITY\"");
            db.execSQL("CREATE TABLE \"COLLECT_PRAISE_ENTITY\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"DEVICE_ID\" TEXT NOT NULL ," + // 1: deviceId
                    "\"PRAISE_ID\" TEXT NOT NULL ," + // 2: praiseId
                    "\"START_TIME\" INTEGER NOT NULL ," + // 3: startTime
                    "\"PRAISE_DATA\" BLOB," + // 4: praiseData
                    "\"COMPLETE_TIME\" INTEGER NOT NULL ," + // 5: completeTime
                    "\"FINISH_TIME\" INTEGER NOT NULL ," + // 6: finishTime
                    "\"TIMEZONE\" TEXT," + // 7: timezone
                    "\"IS_CANCEL\" INTEGER NOT NULL );"); // 8: isCancel
        }
    }

    private static class MigrationV6 implements Migration {

        @Override
        public Integer getVersion() {
            return 6;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE ALARM_CLOCK_ENTITY ADD COLUMN SYNCED INTEGER");
            db.execSQL("ALTER TABLE CONTACT_ENTITY ADD COLUMN SYNCED INTEGER");
            db.execSQL("CREATE TABLE \"USER_ENTITY\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"USER_ID\" TEXT NOT NULL ," + // 1: userId
                    "\"PHONE\" TEXT NOT NULL ," + // 2: phone
                    "\"EMAIL\" TEXT," + // 3: email
                    "\"QQ\" TEXT," + // 4: qq
                    "\"WECHAT\" TEXT," + // 5: wechat
                    "\"SINA_WEIBO\" TEXT," + // 6: sinaWeibo
                    "\"FACEBOOK\" TEXT," + // 7: facebook
                    "\"TWITTER\" TEXT," + // 8: twitter
                    "\"GOOGLE_PLUS\" TEXT," + // 9: googlePlus
                    "\"USER_INFO_DATA\" BLOB);"); // 10: userInfoData
        }
    }

    private static class MigrationV7 implements Migration {

        @Override
        public Integer getVersion() {
            return 7;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN FUNC_MODULE_INFO_DATA BLOB");
        }
    }

    private static class MigrationV8 implements Migration {

        @Override
        public Integer getVersion() {
            return 8;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN UNBIND_CLEAR_LEVEL INTEGER");
        }
    }

    private static class MigrationV9 implements Migration {

        @Override
        public Integer getVersion() {
            return 9;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE CONTACT_ENTITY ADD COLUMN USER_AVATAR TEXT");
            db.execSQL("ALTER TABLE CONTACT_ENTITY ADD COLUMN FRIEND_ID TEXT");
            db.execSQL("ALTER TABLE CONTACT_ENTITY ADD COLUMN FRIEND_NICKNAME TEXT");
            db.execSQL("ALTER TABLE CONTACT_ENTITY ADD COLUMN FRIEND_BABY_AVATAR TEXT");
            db.execSQL("ALTER TABLE CONTACT_ENTITY ADD COLUMN FAMILY_SHORT_NUM TEXT");

            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN LOCATION_MODE INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE DEVICE_ENTITY ADD COLUMN FAMILY_GROUP TEXT");

            db.execSQL("ALTER TABLE BABY_ENTITY RENAME TO _BABY_ENTITY");
            db.execSQL("CREATE TABLE \"BABY_ENTITY\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"USER_ID\" TEXT NOT NULL ," + // 1: userId
                    "\"DEVICE_ID\" TEXT NOT NULL ," + // 2: deviceId
                    "\"PERMISSION\" INTEGER," + // 3: permission
                    "\"RELATION\" TEXT," + // 4: relation
                    "\"BABY_AVATAR\" TEXT," + // 5: babyAvatar
                    "\"NAME\" TEXT," + // 6: name
                    "\"PHONE\" TEXT," + // 7: phone
                    "\"SEX\" INTEGER," + // 8: sex
                    "\"BIRTHDAY\" INTEGER," + // 9: birthday
                    "\"GRADE\" INTEGER," + // 10: grade
                    "\"HEIGHT\" INTEGER," + // 11: height
                    "\"WEIGHT\" INTEGER," + // 12: weight
                    "\"USER_AVATAR\" TEXT," + // 13: userAvatar
                    "\"FAMILY_GROUP\" TEXT," + // 14: familyGroup
                    "\"SMS_AGENT_ENABLED\" INTEGER NOT NULL ," + // 15: smsAgentEnabled
                    "\"NOTIFICATION_CHANNEL\" INTEGER NOT NULL ," + // 16: notificationChannel
                    "\"IS_SELECT\" INTEGER NOT NULL );"); // 17: is_select
            db.execSQL("INSERT INTO BABY_ENTITY (USER_ID, DEVICE_ID, PERMISSION, RELATION, BABY_AVATAR, NAME, PHONE, SEX, BIRTHDAY, GRADE, HEIGHT, WEIGHT, USER_AVATAR, FAMILY_GROUP, SMS_AGENT_ENABLED, NOTIFICATION_CHANNEL, IS_SELECT)" +
                    " SELECT USER_ID, DEVICE_ID, PERMISSION, RELATION, AVATAR, NAME, PHONE, SEX, BIRTHDAY, GRADE, HEIGHT, WEIGHT, NULL, NULL, 0, 0, IS_SELECT FROM _BABY_ENTITY");
            db.execSQL("DROP TABLE _BABY_ENTITY;");

            db.execSQL("ALTER TABLE CHAT_ENTITY RENAME TO _CHAT_ENTITY");
            db.execSQL("CREATE TABLE \"CHAT_ENTITY\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"USER_ID\" TEXT NOT NULL ," + // 1: userId
                    "\"DEVICE_ID\" TEXT," + // 2: deviceId
                    "\"GROUP_ID\" TEXT," + // 3: groupId
                    "\"SENDER_ID\" TEXT NOT NULL ," + // 4: senderId
                    "\"SENDER_TYPE\" INTEGER NOT NULL ," + // 5: senderType
                    "\"TIMESTAMP\" INTEGER NOT NULL ," + // 6: timestamp
                    "\"IS_SEND_DIR\" INTEGER NOT NULL ," + // 7: isSendDir
                    "\"SEND_STATUS\" INTEGER NOT NULL ," + // 8: sendStatus
                    "\"IS_SEEN\" INTEGER NOT NULL ," + // 9: isSeen
                    "\"MESSAGE_TYPE\" INTEGER NOT NULL ," + // 10: messageType
                    "\"TEXT\" TEXT," + // 11: text
                    "\"FILENAME\" TEXT," + // 12: filename
                    "\"FILE_SIZE\" INTEGER NOT NULL ," + // 13: fileSize
                    "\"VOICE_DURATION\" INTEGER NOT NULL ," + // 14: voiceDuration
                    "\"VOICE_IS_PLAYED\" INTEGER NOT NULL ," + // 15: voiceIsPlayed
                    "\"IMAGE_WIDTH\" INTEGER NOT NULL ," + // 16: imageWidth
                    "\"IMAGE_HEIGHT\" INTEGER NOT NULL ," + // 17: imageHeight
                    "\"FILE_UPLOAD_STATUS\" INTEGER NOT NULL ," + // 18: fileUploadStatus
                    "\"EMOTICON\" TEXT);"); // 19: emoticon
            // 发送的语音消息
            db.execSQL("INSERT INTO CHAT_ENTITY (USER_ID, DEVICE_ID, SENDER_ID, SENDER_TYPE, TIMESTAMP, IS_SEND_DIR, SEND_STATUS, IS_SEEN, MESSAGE_TYPE, FILENAME, FILE_SIZE, VOICE_DURATION, VOICE_IS_PLAYED, IMAGE_WIDTH, IMAGE_HEIGHT, FILE_UPLOAD_STATUS)" +
                    " SELECT USER_ID, DEVICE_ID, USER_ID, 1, TIMESTAMP, SEND_OR_REVC, STATUS, IS_READ, MESSAGE_TYPE, FILENAME, FILESIZE, VOICE_TIME, IS_READ, 0, 0, 0 FROM _CHAT_ENTITY WHERE SEND_OR_REVC=1 AND MESSAGE_TYPE=1");
            // 接收的语音消息
            db.execSQL("INSERT INTO CHAT_ENTITY (USER_ID, DEVICE_ID, SENDER_ID, SENDER_TYPE, TIMESTAMP, IS_SEND_DIR, SEND_STATUS, IS_SEEN, MESSAGE_TYPE, FILENAME, FILE_SIZE, VOICE_DURATION, VOICE_IS_PLAYED, IMAGE_WIDTH, IMAGE_HEIGHT, FILE_UPLOAD_STATUS)" +
                    " SELECT USER_ID, DEVICE_ID, DEVICE_ID, 2, TIMESTAMP, SEND_OR_REVC, STATUS, IS_READ, MESSAGE_TYPE, FILENAME, FILESIZE, VOICE_TIME, IS_READ, 0, 0, 0 FROM _CHAT_ENTITY WHERE SEND_OR_REVC=0 AND MESSAGE_TYPE=1");
            // 发送的表情消息
            db.execSQL("INSERT INTO CHAT_ENTITY (USER_ID, DEVICE_ID, SENDER_ID, SENDER_TYPE, TIMESTAMP, IS_SEND_DIR, SEND_STATUS, IS_SEEN, MESSAGE_TYPE, EMOTICON, FILE_SIZE, VOICE_DURATION, VOICE_IS_PLAYED, IMAGE_WIDTH, IMAGE_HEIGHT, FILE_UPLOAD_STATUS)" +
                    " SELECT USER_ID, DEVICE_ID, USER_ID, 1, TIMESTAMP, SEND_OR_REVC, STATUS, IS_READ, MESSAGE_TYPE, CONTENT_EMOJI, 0, 0, 0, 0, 0, 0 FROM _CHAT_ENTITY WHERE SEND_OR_REVC=1 AND MESSAGE_TYPE=2");
            // 接收的表情消息
            db.execSQL("INSERT INTO CHAT_ENTITY (USER_ID, DEVICE_ID, SENDER_ID, SENDER_TYPE, TIMESTAMP, IS_SEND_DIR, SEND_STATUS, IS_SEEN, MESSAGE_TYPE, EMOTICON, FILE_SIZE, VOICE_DURATION, VOICE_IS_PLAYED, IMAGE_WIDTH, IMAGE_HEIGHT, FILE_UPLOAD_STATUS)" +
                    " SELECT USER_ID, DEVICE_ID, DEVICE_ID, 2, TIMESTAMP, SEND_OR_REVC, STATUS, IS_READ, MESSAGE_TYPE, CONTENT_EMOJI, 0, 0, 0, 0, 0, 0 FROM _CHAT_ENTITY WHERE SEND_OR_REVC=0 AND MESSAGE_TYPE=2");
            db.execSQL("DROP TABLE _CHAT_ENTITY;");

            db.execSQL("ALTER TABLE NOTIFY_MESSAGE_ENTITY ADD COLUMN SEQ TEXT");

            db.execSQL("ALTER TABLE SOS_ENTITY ADD COLUMN CALL_ORDER INTEGER NOT NULL DEFAULT 0");

            db.execSQL("CREATE TABLE \"FAMILY_CHAT_GROUP_MEMBER\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"GROUP_ID\" TEXT NOT NULL ," + // 1: groupId
                    "\"DEVICE_ID\" TEXT," + // 2: deviceId
                    "\"BABY_NAME\" TEXT," + // 3: babyName
                    "\"BABY_AVATAR\" TEXT," + // 4: babyAvatar
                    "\"USER_AVATAR\" TEXT," + // 5: userAvatar
                    "\"USER_ID\" TEXT," + // 6: userId
                    "\"PERMISSION\" INTEGER NOT NULL ," + // 7: permission
                    "\"RELATION\" TEXT);"); // 8: relation

            db.execSQL("CREATE TABLE \"SMS_ENTITY\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"SMS_ID\" TEXT NOT NULL ," + // 1: smsId
                    "\"DEVICE_ID\" TEXT NOT NULL ," + // 2: deviceId
                    "\"USER_ID\" TEXT NOT NULL ," + // 3: userId
                    "\"TIME\" INTEGER NOT NULL ," + // 4: time
                    "\"NUMBER\" TEXT NOT NULL ," + // 5: number
                    "\"TEXT\" TEXT NOT NULL ," + // 6: text
                    "\"UNREAD_MARK\" INTEGER NOT NULL ," + // 7: unreadMark
                    "\"SYNCED\" INTEGER NOT NULL );"); // 8: synced
        }
    }

    private static class MigrationV10 implements Migration {

        @Override
        public Integer getVersion() {
            return 10;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE BABY_ENTITY RENAME TO _BABY_ENTITY");
            db.execSQL("CREATE TABLE \"BABY_ENTITY\" (" + //
                    "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                    "\"USER_ID\" TEXT NOT NULL ," + // 1: userId
                    "\"DEVICE_ID\" TEXT NOT NULL ," + // 2: deviceId
                    "\"PERMISSION\" INTEGER NOT NULL ," + // 3: permission
                    "\"RELATION\" TEXT NOT NULL ," + // 4: relation
                    "\"BABY_AVATAR\" TEXT," + // 5: babyAvatar
                    "\"NAME\" TEXT," + // 6: name
                    "\"PHONE\" TEXT," + // 7: phone
                    "\"SEX\" INTEGER NOT NULL ," + // 8: sex
                    "\"BIRTHDAY\" INTEGER NOT NULL ," + // 9: birthday
                    "\"GRADE\" INTEGER NOT NULL ," + // 10: grade
                    "\"HEIGHT\" INTEGER NOT NULL ," + // 11: height
                    "\"WEIGHT\" INTEGER NOT NULL ," + // 12: weight
                    "\"USER_AVATAR\" TEXT," + // 13: userAvatar
                    "\"FAMILY_GROUP\" TEXT," + // 14: familyGroup
                    "\"SMS_AGENT_ENABLED\" INTEGER NOT NULL ," + // 15: smsAgentEnabled
                    "\"NOTIFICATION_CHANNEL\" INTEGER NOT NULL ," + // 16: notificationChannel
                    "\"IS_SELECT\" INTEGER NOT NULL );"); // 17: is_select
            db.execSQL("INSERT INTO BABY_ENTITY (USER_ID, DEVICE_ID, PERMISSION, RELATION, BABY_AVATAR, NAME, PHONE, SEX, BIRTHDAY, GRADE, HEIGHT, WEIGHT, USER_AVATAR, FAMILY_GROUP, SMS_AGENT_ENABLED, NOTIFICATION_CHANNEL, IS_SELECT)" +
                    " SELECT USER_ID, DEVICE_ID, IFNULL(PERMISSION, 0) PERMISSION, IFNULL(RELATION, '') RELATION, BABY_AVATAR, NAME, PHONE, IFNULL(SEX, 0) SEX, IFNULL(BIRTHDAY, 0) BIRTHDAY, IFNULL(GRADE, 0) GRADE, IFNULL(HEIGHT, 0) HEIGHT, IFNULL(WEIGHT, 0) WEIGHT, USER_AVATAR, FAMILY_GROUP, SMS_AGENT_ENABLED, NOTIFICATION_CHANNEL, IS_SELECT FROM _BABY_ENTITY");
            db.execSQL("DROP TABLE _BABY_ENTITY;");
        }
    }
}