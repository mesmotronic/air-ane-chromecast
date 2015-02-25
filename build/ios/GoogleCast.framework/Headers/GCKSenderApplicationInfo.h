// Copyright 2013 Google Inc.

#import <Foundation/Foundation.h>

#import "GCKDefines.h"

typedef NS_ENUM(NSInteger, GCKSenderApplicationInfoPlatform) {
  GCKSenderApplicationInfoPlatformAndroid = 1,
  GCKSenderApplicationInfoPlatformiOS = 2,
  GCKSenderApplicationInfoPlatformChrome = 3,
};

/**
 * Container class for information about a sender application.
 *
 * @ingroup DeviceControl
 */
GCK_EXPORT
@interface GCKSenderApplicationInfo : NSObject<NSCopying>

/** The sender app's platform. */
@property(nonatomic, readonly) GCKSenderApplicationInfoPlatform platform;

/** The sender app's unique identifier. */
@property(nonatomic, copy, readonly) NSString *appIdentifier;

/** The sender app's launch URL. */
@property(nonatomic, strong, readonly) NSURL *launchURL;

/** @cond INTERNAL */

/**
 * Initalizes this GCKImage from its JSON representation.
 */
- (id)initWithJSONObject:(id)JSONObject;

/**
 * Create a JSON object which can serialized with NSJSONSerialization to pass to the receiver.
 */
- (id)JSONObject;

/** @endcond */

@end

