// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: src/com/linbit/drbdmanage/proto/MsgHeader.proto

package com.linbit.drbdmanage.proto;

public final class MsgHeaderOuterClass {
  private MsgHeaderOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface MsgHeaderOrBuilder extends
      // @@protoc_insertion_point(interface_extends:com.linbit.drbdmanage.proto.MsgHeader)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * Identifying number for this message
     * Immediate answers to this message will be sent
     * back with the same msg_id
     * </pre>
     *
     * <code>int32 msg_id = 1;</code>
     */
    int getMsgId();

    /**
     * <pre>
     * Name of the API call to execute
     * The message content (parameters) following this
     * message header will be processed by the API method
     * that is selected by this field
     * </pre>
     *
     * <code>string api_call = 2;</code>
     */
    java.lang.String getApiCall();
    /**
     * <pre>
     * Name of the API call to execute
     * The message content (parameters) following this
     * message header will be processed by the API method
     * that is selected by this field
     * </pre>
     *
     * <code>string api_call = 2;</code>
     */
    com.google.protobuf.ByteString
        getApiCallBytes();
  }
  /**
   * <pre>
   * Message header
   * </pre>
   *
   * Protobuf type {@code com.linbit.drbdmanage.proto.MsgHeader}
   */
  public  static final class MsgHeader extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:com.linbit.drbdmanage.proto.MsgHeader)
      MsgHeaderOrBuilder {
    // Use MsgHeader.newBuilder() to construct.
    private MsgHeader(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private MsgHeader() {
      msgId_ = 0;
      apiCall_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }
    private MsgHeader(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      int mutable_bitField0_ = 0;
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!input.skipField(tag)) {
                done = true;
              }
              break;
            }
            case 8: {

              msgId_ = input.readInt32();
              break;
            }
            case 18: {
              java.lang.String s = input.readStringRequireUtf8();

              apiCall_ = s;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.linbit.drbdmanage.proto.MsgHeaderOuterClass.internal_static_com_linbit_drbdmanage_proto_MsgHeader_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.linbit.drbdmanage.proto.MsgHeaderOuterClass.internal_static_com_linbit_drbdmanage_proto_MsgHeader_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader.class, com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader.Builder.class);
    }

    public static final int MSG_ID_FIELD_NUMBER = 1;
    private int msgId_;
    /**
     * <pre>
     * Identifying number for this message
     * Immediate answers to this message will be sent
     * back with the same msg_id
     * </pre>
     *
     * <code>int32 msg_id = 1;</code>
     */
    public int getMsgId() {
      return msgId_;
    }

    public static final int API_CALL_FIELD_NUMBER = 2;
    private volatile java.lang.Object apiCall_;
    /**
     * <pre>
     * Name of the API call to execute
     * The message content (parameters) following this
     * message header will be processed by the API method
     * that is selected by this field
     * </pre>
     *
     * <code>string api_call = 2;</code>
     */
    public java.lang.String getApiCall() {
      java.lang.Object ref = apiCall_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        apiCall_ = s;
        return s;
      }
    }
    /**
     * <pre>
     * Name of the API call to execute
     * The message content (parameters) following this
     * message header will be processed by the API method
     * that is selected by this field
     * </pre>
     *
     * <code>string api_call = 2;</code>
     */
    public com.google.protobuf.ByteString
        getApiCallBytes() {
      java.lang.Object ref = apiCall_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        apiCall_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (msgId_ != 0) {
        output.writeInt32(1, msgId_);
      }
      if (!getApiCallBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, apiCall_);
      }
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (msgId_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(1, msgId_);
      }
      if (!getApiCallBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, apiCall_);
      }
      memoizedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader)) {
        return super.equals(obj);
      }
      com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader other = (com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader) obj;

      boolean result = true;
      result = result && (getMsgId()
          == other.getMsgId());
      result = result && getApiCall()
          .equals(other.getApiCall());
      return result;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + MSG_ID_FIELD_NUMBER;
      hash = (53 * hash) + getMsgId();
      hash = (37 * hash) + API_CALL_FIELD_NUMBER;
      hash = (53 * hash) + getApiCall().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * <pre>
     * Message header
     * </pre>
     *
     * Protobuf type {@code com.linbit.drbdmanage.proto.MsgHeader}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:com.linbit.drbdmanage.proto.MsgHeader)
        com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeaderOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.linbit.drbdmanage.proto.MsgHeaderOuterClass.internal_static_com_linbit_drbdmanage_proto_MsgHeader_descriptor;
      }

      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.linbit.drbdmanage.proto.MsgHeaderOuterClass.internal_static_com_linbit_drbdmanage_proto_MsgHeader_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader.class, com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader.Builder.class);
      }

      // Construct using com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      public Builder clear() {
        super.clear();
        msgId_ = 0;

        apiCall_ = "";

        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.linbit.drbdmanage.proto.MsgHeaderOuterClass.internal_static_com_linbit_drbdmanage_proto_MsgHeader_descriptor;
      }

      public com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader getDefaultInstanceForType() {
        return com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader.getDefaultInstance();
      }

      public com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader build() {
        com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader buildPartial() {
        com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader result = new com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader(this);
        result.msgId_ = msgId_;
        result.apiCall_ = apiCall_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader) {
          return mergeFrom((com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader other) {
        if (other == com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader.getDefaultInstance()) return this;
        if (other.getMsgId() != 0) {
          setMsgId(other.getMsgId());
        }
        if (!other.getApiCall().isEmpty()) {
          apiCall_ = other.apiCall_;
          onChanged();
        }
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int msgId_ ;
      /**
       * <pre>
       * Identifying number for this message
       * Immediate answers to this message will be sent
       * back with the same msg_id
       * </pre>
       *
       * <code>int32 msg_id = 1;</code>
       */
      public int getMsgId() {
        return msgId_;
      }
      /**
       * <pre>
       * Identifying number for this message
       * Immediate answers to this message will be sent
       * back with the same msg_id
       * </pre>
       *
       * <code>int32 msg_id = 1;</code>
       */
      public Builder setMsgId(int value) {
        
        msgId_ = value;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * Identifying number for this message
       * Immediate answers to this message will be sent
       * back with the same msg_id
       * </pre>
       *
       * <code>int32 msg_id = 1;</code>
       */
      public Builder clearMsgId() {
        
        msgId_ = 0;
        onChanged();
        return this;
      }

      private java.lang.Object apiCall_ = "";
      /**
       * <pre>
       * Name of the API call to execute
       * The message content (parameters) following this
       * message header will be processed by the API method
       * that is selected by this field
       * </pre>
       *
       * <code>string api_call = 2;</code>
       */
      public java.lang.String getApiCall() {
        java.lang.Object ref = apiCall_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          apiCall_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <pre>
       * Name of the API call to execute
       * The message content (parameters) following this
       * message header will be processed by the API method
       * that is selected by this field
       * </pre>
       *
       * <code>string api_call = 2;</code>
       */
      public com.google.protobuf.ByteString
          getApiCallBytes() {
        java.lang.Object ref = apiCall_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          apiCall_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <pre>
       * Name of the API call to execute
       * The message content (parameters) following this
       * message header will be processed by the API method
       * that is selected by this field
       * </pre>
       *
       * <code>string api_call = 2;</code>
       */
      public Builder setApiCall(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        apiCall_ = value;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * Name of the API call to execute
       * The message content (parameters) following this
       * message header will be processed by the API method
       * that is selected by this field
       * </pre>
       *
       * <code>string api_call = 2;</code>
       */
      public Builder clearApiCall() {
        
        apiCall_ = getDefaultInstance().getApiCall();
        onChanged();
        return this;
      }
      /**
       * <pre>
       * Name of the API call to execute
       * The message content (parameters) following this
       * message header will be processed by the API method
       * that is selected by this field
       * </pre>
       *
       * <code>string api_call = 2;</code>
       */
      public Builder setApiCallBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        apiCall_ = value;
        onChanged();
        return this;
      }
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }

      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }


      // @@protoc_insertion_point(builder_scope:com.linbit.drbdmanage.proto.MsgHeader)
    }

    // @@protoc_insertion_point(class_scope:com.linbit.drbdmanage.proto.MsgHeader)
    private static final com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader();
    }

    public static com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<MsgHeader>
        PARSER = new com.google.protobuf.AbstractParser<MsgHeader>() {
      public MsgHeader parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
          return new MsgHeader(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<MsgHeader> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<MsgHeader> getParserForType() {
      return PARSER;
    }

    public com.linbit.drbdmanage.proto.MsgHeaderOuterClass.MsgHeader getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_com_linbit_drbdmanage_proto_MsgHeader_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_com_linbit_drbdmanage_proto_MsgHeader_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n/src/com/linbit/drbdmanage/proto/MsgHea" +
      "der.proto\022\033com.linbit.drbdmanage.proto\"-" +
      "\n\tMsgHeader\022\016\n\006msg_id\030\001 \001(\005\022\020\n\010api_call\030" +
      "\002 \001(\tb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_com_linbit_drbdmanage_proto_MsgHeader_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_com_linbit_drbdmanage_proto_MsgHeader_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_com_linbit_drbdmanage_proto_MsgHeader_descriptor,
        new java.lang.String[] { "MsgId", "ApiCall", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
