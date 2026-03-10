#import <AVFoundation/AVFoundation.h>

#include "platform/macos/speech.h"
#include "options.h" // Include options.h to access speechRate


namespace devilution {

static AVSpeechSynthesizer *synthesizer;

void osx_init_speech()
{
	synthesizer = [[AVSpeechSynthesizer alloc] init];
}

void osx_shutdown_speech()
{
	[synthesizer release];
}

void osx_speak_text(std::string_view text, bool force)
{
	// Regardless of the 'force' flag, implement Tolk's always-interrupting behavior.
	// Stop any current speech and clear the queue before speaking the new utterance.
	[synthesizer stopSpeakingAtBoundary:AVSpeechBoundaryImmediate];
    // Dynamically call cancelSpeaking if available to clear the queue
    if ([synthesizer respondsToSelector:@selector(cancelSpeaking)]) {
        [synthesizer performSelector:@selector(cancelSpeaking)];
    }

	AVSpeechUtterance *utterance = [AVSpeechUtterance speechUtteranceWithString:[NSString stringWithUTF8String:text.data()]];
    utterance.rate = GetOptions().Audio.speechRate.GetValue(); // Set the speech rate
	[synthesizer speakUtterance:utterance];
}

} // namespace devilution
