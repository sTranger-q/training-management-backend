#!/usr/bin/env python3
"""
IDEA 运行配置自动生成脚本
在本地执行此脚本，会自动写入 .idea/runConfigurations/ 下的两套配置：
  1) TrainingBackend Dev (H2)
  2) TrainingBackend Prod (MySQL)

用法：
  cd training-management-backend
  python3 setup_idea_run_configs.py

执行后重启 IDEA 即可在顶部运行配置下拉框看到配置。
"""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent
TARGET = ROOT / ".idea" / "runConfigurations"
TARGET.mkdir(parents=True, exist_ok=True)

DEV_XML = r'''<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="TrainingBackend Dev (H2)" type="Application" factoryName="Application">
    <option name="MAIN_CLASS_NAME" value="com.training.system.TrainingApplication" />
    <option name="VM_PARAMETERS" value="-Dspring.profiles.active=dev -Dfile.encoding=UTF-8" />
    <option name="PROGRAM_PARAMETERS" value="" />
    <option name="WORKING_DIRECTORY" value="$PROJECT_DIR$" />
    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
    <option name="ALTERNATIVE_JRE_PATH" value="" />
    <envs>
      <env name="SPRING_PROFILES_ACTIVE" value="dev" />
      <env name="SERVER_PORT" value="8080" />
    </envs>
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
'''

PROD_XML = r'''<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="TrainingBackend Prod (MySQL)" type="Application" factoryName="Application">
    <option name="MAIN_CLASS_NAME" value="com.training.system.TrainingApplication" />
    <option name="VM_PARAMETERS" value="-Dspring.profiles.active=prod -Dfile.encoding=UTF-8" />
    <option name="PROGRAM_PARAMETERS" value="" />
    <option name="WORKING_DIRECTORY" value="$PROJECT_DIR$" />
    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
    <option name="ALTERNATIVE_JRE_PATH" value="" />
    <envs>
      <env name="SPRING_PROFILES_ACTIVE" value="prod" />
      <env name="SERVER_PORT" value="8080" />
      <env name="DB_HOST" value="localhost" />
      <env name="DB_PORT" value="3306" />
      <env name="DB_NAME" value="training" />
      <env name="DB_USERNAME" value="root" />
      <env name="DB_PASSWORD" value="" />
    </envs>
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
'''

(ROOT / ".idea" / "runConfigurations" / "TrainingBackend_Dev_H2.xml").write_text(DEV_XML, encoding="utf-8")
(ROOT / ".idea" / "runConfigurations" / "TrainingBackend_Prod_MySQL.xml").write_text(PROD_XML, encoding="utf-8")

print(f"已生成 IDEA 运行配置到：{TARGET}")
print("请重启 IDEA 或 File → Reload All from Disk，即可在运行配置下拉框中看到：")
print("  1) TrainingBackend Dev (H2)   - 开发环境（H2 内存库，一键起）")
print("  2) TrainingBackend Prod (MySQL) - 生产环境（本地 MySQL，DB_PASSWORD 请改为你的密码）")
