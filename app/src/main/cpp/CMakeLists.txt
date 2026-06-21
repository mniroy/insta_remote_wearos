cmake_minimum_required(VERSION 3.10.2)
project("cpu_monitor")

add_library(cpu_monitor SHARED cpu_sampler.cpp)

find_library(log-lib log)
target_link_libraries(cpu_monitor ${log-lib})
