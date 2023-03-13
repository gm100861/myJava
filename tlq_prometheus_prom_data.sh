#!/bin/bash
# Author Hongwei.Liu
# Desc: 该脚本用于采集东方通消息队列(TLQ)的一些队列数据, 主要包括以下
# 1. 就绪状态的消息数
# 2. 正在发送状态的消息数
# 3. 正在接收状态的消息数
# 4. 等待确认应答状态的消息数
# 5. 接收应用消息的个数
# 6. 队列的最大容量
# 7. TLQ License到期时间
# 采集以上数据之后, 写入文件到指定的prom中, 以便与Prometheus集成

# 一些全局变量定义
tlq_home='/opt/TLQ8'
source $tlq_home/setp
prom_file='/opt/TLQ8/tlq.prom'

# 判断TLQ是否启动, 如果没启动退出程序
if [[ $($tlq_home/bin/tlqready) != 'y' ]]; then
  echo 'TLQ IS DOWN'
  exit 1;
fi

# 清空上次执行prom文件的数据
echo > $prom_file

# 定义函数, 获取指定topic的一些常用参数, 前面注释中的前6项
function gen_prometheus_data() {
  queue_name=$1
  queue_status=$($tlq_home/bin/tlqstat -qcu qcu1 -c -ct 1 | sed -n "/$queue_name/"p)
  queue_max_capacity=$($tlq_home/bin/tlqstat -msg qcu1 queuef -ct 1 | sed -n '/MsgDesMax/'p | awk '{gsub("MsgDesMax=\\[","",$4);gsub("\\]", "", $4); print $4}')
  queue_ready_quantity=$(echo "$queue_status" | awk '{print $5}')
  queue_sdning_quantity=$(echo "$queue_status" | awk '{print $6}')
  queue_rcving_quantity=$(echo "$queue_status" | awk '{print $7}')
  queue_waitack_quantity=$(echo "$queue_status" | awk '{print $8}')
  queue_getor_quantity=$(echo "$queue_status" | awk '{print $9}')
  tlq_queue_ready_status="tlq_${queue_name}_ready_quantity $queue_ready_quantity"
  tlq_queue_sdning_status="tlq_${queue_name}_sdning_quantity $queue_sdning_quantity"
  tlq_queue_rcving_status="tlq_${queue_name}_rcving_quantity $queue_rcving_quantity"
  tlq_queue_waitack_status="tlq_${queue_name}_waitack_quantity $queue_waitack_quantity"
  tlq_queue_getor_status="tlq_${queue_name}_getor_quantity $queue_getor_quantity"
  tlq_max_capacity="tlq_${queue_name}_queue_max_capacity $queue_max_capacity"
  {
    echo '# HELP tlq_'"$queue_name"'_ready_quantity 就绪状态的消息数'
    echo '# TYPE tlq_'"$queue_name"'_ready_quantity gauge'
    echo "$tlq_queue_ready_status"

    echo '# HELP tlq_'"$queue_name"'_sdning_quantity 正在发送状态的消息数'
    echo '# TYPE tlq_'"$queue_name"'_sdning_quantity gauge'
    echo "$tlq_queue_sdning_status"

    echo '# HELP tlq_'"$queue_name"'_rcving_quantity 正在接收状态的消息数'
    echo '# TYPE tlq_'"$queue_name"'_rcving_quantity gauge'
    echo "$tlq_queue_rcving_status"

    echo '# HELP tlq_'"$queue_name"'_waitack_quantity 等待确认应答状态的消息数'
    echo '# TYPE tlq_'"$queue_name"'_waitack_quantity gauge'
    echo "$tlq_queue_waitack_status"

    echo '# HELP tlq_'"$queue_name"'_getor_quantity 接收应用消息的个数'
    echo '# TYPE tlq_'"$queue_name"'_getor_quantity gauge'
    echo "$tlq_queue_getor_status"

    echo '# HELP tlq_'"$queue_name"'_queue_max_capacity 队列的最大容量'
    echo '# TYPE tlq_'"$queue_name"'_queue_max_capacity gauge'
    echo "$tlq_max_capacity"
  } >> $prom_file

}

# 要处理的topic, 这里只处理了 queueh queuef queuea三个, 如果有多的直接空格分隔加上即可
for queue_name in queueh queuef queuea; do
  gen_prometheus_data $queue_name;
done

# license过期时间计算
# shellcheck disable=SC2046
expire_remain_days=$((($(date +%s -d $($tlq_home/bin/tlqstat -lic -d | sed -n '/Expire Date/'p | awk '{gsub("=\\[", "", $3);gsub("-","",$3);print $3}')) - $(date +%s))/86400));
{
    echo ' #HELP tlq_expire_remain_days TLQ过期剩余天数'
    echo '# TYPE tlq_expire_remain_days gauge'
    echo "tlq_expire_remain_days" $expire_remain_days
} >> $prom_file
