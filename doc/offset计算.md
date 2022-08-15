GcRoot<xxx>
GcRoot<xxx>
GcRoot<xxx>
uint32_t access_flags_;

这样 ART_ACCESS_FLAG_OFFSET偏移值就是12


GcRoot<xxx>
GcRoot<xxx>
GcRoot<xxx>
GcRoot<xxx>
uint32_t access_flags_;

这样 ART_ACCESS_FLAG_OFFSET偏移值就是16




=================================
protected:
  // 4
  GcRoot<mirror::Class> declaring_class_;
  //
  std::atomic<std::uint32_t> access_flags_;

declaring_class_->dex_cache_ */

  uint32_t dex_code_item_offset_;

  uint32_t dex_method_index_;
  uint16_t method_index_;

  union {
    uint16_t hotness_count_;
    uint16_t imt_index_;
  };

  struct PtrSizedFields {
    void* data_;
    void* entry_point_from_quick_compiled_code_;
  } ptr_sized_fields_;

-----
  GcRoot 4 bytes
  uint32_t 4 bytes
  uint32_t 4 bytes
  uint16_t 2 bytes
  union {uint16_t   uint16_t} 2 bytes
  struct{void* void* }看2指针大小

---用lldb去看，然后反算偏移


https://github.com/canyie/pine/blob/master/core/src/main/cpp/art/art_method.cpp#L112
