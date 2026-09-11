-- =============================================================================
-- ApexSales · DLYK CRM 建库建表脚本（纯结构，不含数据）
-- 由 mysqldump 全量快照拆分而来：结构在此文件，假数据见 ApexSales_data.sql
-- 导入：mysql -u root -p < ApexSales.sql && mysql -u root -p < ApexSales_data.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `dlyk` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `dlyk`;

-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: dlyk
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `t_activity`
--

DROP TABLE IF EXISTS `t_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，活动ID',
  `owner_id` int DEFAULT NULL COMMENT '活动所属人ID',
  `name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '活动名称',
  `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
  `cost` decimal(11,2) DEFAULT NULL COMMENT '活动预算',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '活动描述',
  `create_time` datetime DEFAULT NULL COMMENT '活动创建时间',
  `create_by` int DEFAULT NULL COMMENT '活动创建人',
  `edit_time` datetime DEFAULT NULL COMMENT '活动编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '活动编辑人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `owner` (`owner_id`) USING BTREE,
  KEY `create_by` (`create_by`) USING BTREE,
  KEY `edit_by` (`edit_by`) USING BTREE,
  CONSTRAINT `t_activity_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_activity_ibfk_2` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_activity_ibfk_3` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='市场活动表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_activity_remark`
--

DROP TABLE IF EXISTS `t_activity_remark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity_remark` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，活动备注ID',
  `activity_id` int DEFAULT NULL COMMENT '活动ID',
  `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注内容',
  `create_time` datetime DEFAULT NULL COMMENT '备注创建时间',
  `create_by` int DEFAULT NULL COMMENT '备注创建人',
  `edit_time` datetime DEFAULT NULL COMMENT '备注编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '备注编辑人',
  `deleted` int DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `activity_id` (`activity_id`) USING BTREE,
  KEY `t_activity_remark_ibfk_2` (`create_by`) USING BTREE,
  KEY `t_activity_remark_ibfk_3` (`edit_by`) USING BTREE,
  CONSTRAINT `t_activity_remark_ibfk_1` FOREIGN KEY (`activity_id`) REFERENCES `t_activity` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_activity_remark_ibfk_2` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_activity_remark_ibfk_3` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='市场活动备注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_ai_payment_order`
--

DROP TABLE IF EXISTS `t_ai_payment_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_ai_payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL COMMENT 'AIyyyyMMddHHmmss + ',
  `user_id` int NOT NULL COMMENT 'IDt_user.id',
  `ability_key` varchar(64) NOT NULL COMMENT ' AiAbility  key deep_analysis',
  `ability_name` varchar(64) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 1 2',
  `paid_time` datetime DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL COMMENT '(=paid_time+30)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id_status` (`user_id`,`status`),
  KEY `idx_user_ability` (`user_id`,`ability_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_clue`
--

DROP TABLE IF EXISTS `t_clue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_clue` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，线索ID',
  `owner_id` int DEFAULT NULL COMMENT '线索所属人ID',
  `activity_id` int DEFAULT NULL COMMENT '活动ID',
  `full_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '姓名',
  `appellation` int DEFAULT NULL COMMENT '称呼',
  `phone` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `weixin` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '微信号',
  `qq` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'QQ号',
  `email` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮箱',
  `age` int DEFAULT NULL COMMENT '年龄',
  `job` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '职业',
  `year_income` decimal(10,2) DEFAULT NULL COMMENT '年收入',
  `address` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '地址',
  `need_loan` int DEFAULT NULL COMMENT '是否需要贷款（0不需要，1需要）',
  `intention_state` int DEFAULT NULL COMMENT '意向状态',
  `intention_product` int DEFAULT NULL COMMENT '意向产品',
  `state` int DEFAULT NULL COMMENT '线索状态',
  `source` int DEFAULT NULL COMMENT '线索来源',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '线索描述',
  `next_contact_time` datetime DEFAULT NULL COMMENT '下次联系时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` int DEFAULT NULL COMMENT '创建人',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `appellation` (`appellation`) USING BTREE,
  KEY `state` (`state`) USING BTREE,
  KEY `source` (`source`) USING BTREE,
  KEY `owner` (`owner_id`) USING BTREE,
  KEY `create_by` (`create_by`) USING BTREE,
  KEY `edit_by` (`edit_by`) USING BTREE,
  KEY `t_clue_ibfk_7` (`activity_id`) USING BTREE,
  KEY `t_clue_ibfk_8` (`need_loan`) USING BTREE,
  KEY `t_clue_ibfk_9` (`intention_state`) USING BTREE,
  KEY `t_clue_ibfk_10` (`intention_product`) USING BTREE,
  CONSTRAINT `t_clue_ibfk_1` FOREIGN KEY (`appellation`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_10` FOREIGN KEY (`intention_product`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_2` FOREIGN KEY (`state`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_3` FOREIGN KEY (`source`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_4` FOREIGN KEY (`owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_5` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_6` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_7` FOREIGN KEY (`activity_id`) REFERENCES `t_activity` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_8` FOREIGN KEY (`need_loan`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_ibfk_9` FOREIGN KEY (`intention_state`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1144 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='线索表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_clue_remark`
--

DROP TABLE IF EXISTS `t_clue_remark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_clue_remark` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，线索备注ID',
  `clue_id` int DEFAULT NULL COMMENT '线索ID',
  `note_way` int DEFAULT NULL COMMENT '跟踪方式',
  `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '跟踪内容',
  `create_time` datetime DEFAULT NULL COMMENT '跟踪时间',
  `create_by` int DEFAULT NULL COMMENT '跟踪人',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  `deleted` int DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `create_by` (`create_by`) USING BTREE,
  KEY `edit_by` (`edit_by`) USING BTREE,
  KEY `clue_id` (`clue_id`) USING BTREE,
  KEY `t_clue_remark_ibfk_4` (`note_way`) USING BTREE,
  CONSTRAINT `t_clue_remark_ibfk_1` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_remark_ibfk_2` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_remark_ibfk_3` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_clue_remark_ibfk_4` FOREIGN KEY (`note_way`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='线索跟踪记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_customer`
--

DROP TABLE IF EXISTS `t_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_customer` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，客户ID',
  `clue_id` int DEFAULT NULL COMMENT '线索ID',
  `product` int DEFAULT NULL COMMENT '选购产品',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '客户描述',
  `next_contact_time` datetime DEFAULT NULL COMMENT '下次联系时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` int DEFAULT NULL COMMENT '创建人',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_customer_ibfk_1` (`clue_id`) USING BTREE,
  KEY `t_customer_ibfk_2` (`product`) USING BTREE,
  KEY `t_customer_ibfk_3` (`create_by`) USING BTREE,
  KEY `t_customer_ibfk_4` (`edit_by`) USING BTREE,
  CONSTRAINT `t_customer_ibfk_1` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_customer_ibfk_2` FOREIGN KEY (`product`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_customer_ibfk_3` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_customer_ibfk_4` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='客户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_customer_remark`
--

DROP TABLE IF EXISTS `t_customer_remark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_customer_remark` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，客户备注ID',
  `customer_id` int DEFAULT NULL COMMENT '客户ID',
  `note_way` int DEFAULT NULL COMMENT '跟踪方式',
  `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '跟踪内容',
  `create_by` int DEFAULT NULL COMMENT '跟踪人',
  `create_time` datetime DEFAULT NULL COMMENT '跟踪时间',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  `deleted` int DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_customer_remark_ibfk_1` (`customer_id`) USING BTREE,
  KEY `t_customer_remark_ibfk_2` (`note_way`) USING BTREE,
  KEY `t_customer_remark_ibfk_3` (`create_by`) USING BTREE,
  KEY `t_customer_remark_ibfk_4` (`edit_by`) USING BTREE,
  CONSTRAINT `t_customer_remark_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_customer_remark_ibfk_2` FOREIGN KEY (`note_way`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_customer_remark_ibfk_3` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_customer_remark_ibfk_4` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='客户跟踪记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_dic_type`
--

DROP TABLE IF EXISTS `t_dic_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_dic_type` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，字典类型ID',
  `type_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典类型代码',
  `type_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典类型名称',
  `remark` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code` (`type_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_dic_value`
--

DROP TABLE IF EXISTS `t_dic_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_dic_value` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，字典值ID',
  `type_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典类型代码',
  `type_value` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典值',
  `order` int DEFAULT NULL COMMENT '字典值排序',
  `remark` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_dic_value_ibfk_1` (`type_code`) USING BTREE,
  CONSTRAINT `t_dic_value_ibfk_1` FOREIGN KEY (`type_code`) REFERENCES `t_dic_type` (`type_code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='字典值表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_permission`
--

DROP TABLE IF EXISTS `t_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `url` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parent_id` int DEFAULT NULL,
  `order_no` int DEFAULT NULL,
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_product`
--

DROP TABLE IF EXISTS `t_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_product` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，线索ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '产品名称',
  `guide_price_s` decimal(10,2) DEFAULT NULL COMMENT '官方指导起始价',
  `guide_price_e` decimal(10,2) DEFAULT NULL COMMENT '官方指导最高价',
  `quotation` decimal(10,2) DEFAULT NULL COMMENT '经销商报价',
  `state` int DEFAULT NULL COMMENT '状态 0在售 1售罄',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` int DEFAULT NULL COMMENT '创建人',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_product_ibfk_1` (`create_by`) USING BTREE,
  KEY `t_product_ibfk_2` (`edit_by`) USING BTREE,
  CONSTRAINT `t_product_ibfk_1` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_product_ibfk_2` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='产品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_role`
--

DROP TABLE IF EXISTS `t_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_role_permission`
--

DROP TABLE IF EXISTS `t_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_role_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int DEFAULT NULL,
  `permission_id` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_role_permission_ibfk_1` (`role_id`) USING BTREE,
  KEY `t_role_permission_ibfk_2` (`permission_id`) USING BTREE,
  CONSTRAINT `t_role_permission_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_role_permission_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `t_permission` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色权限关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_system_info`
--

DROP TABLE IF EXISTS `t_system_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_system_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `system_code` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `site` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `logo` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `title` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `description` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `keywords` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `shortcuticon` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `tel` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `weixin` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `email` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `address` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `version` varchar(145) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `closeMsg` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `isopen` varchar(8) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'y',
  `create_time` datetime DEFAULT NULL,
  `create_by` int DEFAULT NULL,
  `edit_time` datetime DEFAULT NULL,
  `edit_by` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_system_info_ibfk_1` (`create_by`) USING BTREE,
  KEY `t_system_info_ibfk_2` (`edit_by`) USING BTREE,
  CONSTRAINT `t_system_info_ibfk_1` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_system_info_ibfk_2` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='系统信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_tran`
--

DROP TABLE IF EXISTS `t_tran`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_tran` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易ID',
  `tran_no` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '交易流水号',
  `customer_id` int DEFAULT NULL COMMENT '客户ID',
  `money` decimal(10,2) DEFAULT NULL COMMENT '交易金额',
  `expected_date` datetime DEFAULT NULL COMMENT '预计成交日期',
  `stage` int DEFAULT NULL COMMENT '交易所处阶段',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '交易描述',
  `next_contact_time` datetime DEFAULT NULL COMMENT '下次联系时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` int DEFAULT NULL COMMENT '创建人',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_tran_ibfk_1` (`customer_id`) USING BTREE,
  KEY `t_tran_ibfk_2` (`stage`) USING BTREE,
  KEY `t_tran_ibfk_3` (`create_by`) USING BTREE,
  KEY `t_tran_ibfk_4` (`edit_by`) USING BTREE,
  CONSTRAINT `t_tran_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_ibfk_2` FOREIGN KEY (`stage`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_ibfk_3` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_ibfk_4` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='交易表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_tran_history`
--

DROP TABLE IF EXISTS `t_tran_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_tran_history` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易记录ID',
  `tran_id` int DEFAULT NULL COMMENT '交易ID',
  `stage` int DEFAULT NULL COMMENT '交易阶段',
  `money` decimal(10,2) DEFAULT NULL COMMENT '交易金额',
  `expected_date` datetime DEFAULT NULL COMMENT '交易预计成交时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` int DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_tran_history_ibfk_1` (`tran_id`) USING BTREE,
  KEY `t_tran_history_ibfk_2` (`stage`) USING BTREE,
  KEY `t_tran_history_ibfk_3` (`create_by`) USING BTREE,
  CONSTRAINT `t_tran_history_ibfk_1` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_history_ibfk_2` FOREIGN KEY (`stage`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_history_ibfk_3` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='交易历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_tran_remark`
--

DROP TABLE IF EXISTS `t_tran_remark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_tran_remark` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易备注ID',
  `tran_id` int DEFAULT NULL COMMENT '交易ID',
  `note_way` int DEFAULT NULL COMMENT '跟踪方式',
  `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '跟踪内容',
  `create_time` datetime DEFAULT NULL COMMENT '跟踪时间',
  `create_by` int DEFAULT NULL COMMENT '跟踪人',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  `deleted` int DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_tran_remark_ibfk_1` (`tran_id`) USING BTREE,
  KEY `t_tran_remark_ibfk_2` (`note_way`) USING BTREE,
  KEY `t_tran_remark_ibfk_3` (`create_by`) USING BTREE,
  KEY `t_tran_remark_ibfk_4` (`edit_by`) USING BTREE,
  CONSTRAINT `t_tran_remark_ibfk_1` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_remark_ibfk_2` FOREIGN KEY (`note_way`) REFERENCES `t_dic_value` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_remark_ibfk_3` FOREIGN KEY (`create_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_tran_remark_ibfk_4` FOREIGN KEY (`edit_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='交易跟踪记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，用户ID',
  `login_act` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '登录账号',
  `login_pwd` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '登录密码',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户姓名',
  `phone` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户手机',
  `email` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户邮箱',
  `account_no_expired` int DEFAULT NULL COMMENT '账户是否没有过期，0已过期 1正常',
  `credentials_no_expired` int DEFAULT NULL COMMENT '密码是否没有过期，0已过期 1正常',
  `account_no_locked` int DEFAULT NULL COMMENT '账号是否没有锁定，0已锁定 1正常',
  `account_enabled` int DEFAULT NULL COMMENT '账号是否启用，0禁用 1启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` int DEFAULT NULL COMMENT '创建人',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_by` int DEFAULT NULL COMMENT '编辑人',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `login_act` (`login_act`) USING BTREE,
  UNIQUE KEY `phone` (`phone`) USING BTREE,
  UNIQUE KEY `email` (`email`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--


--
-- Table structure for table `t_user_role`
--

DROP TABLE IF EXISTS `t_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `role_id` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `t_user_role_ibfk_1` (`user_id`) USING BTREE,
  KEY `t_user_role_ibfk_2` (`role_id`) USING BTREE,
  CONSTRAINT `t_user_role_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `t_user_role_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户角色关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
--

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

