// Supabase Edge Function: send-notification
// Sends push notifications via Firebase Cloud Messaging (FCM)
//
// Deploy: supabase functions deploy send-notification
//
// Environment variables required:
// - FCM_SERVER_KEY: Firebase Cloud Messaging server key

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const FCM_URL = "https://fcm.googleapis.com/fcm/send"
const FCM_SERVER_KEY = Deno.env.get('FCM_SERVER_KEY')

interface NotificationRequest {
  user_id: string
  title: string
  body: string
  data?: Record<string, string>
}

serve(async (req) => {
  try {
    // Parse request body
    const { user_id, title, body, data }: NotificationRequest = await req.json()

    // Validate input
    if (!user_id || !title || !body) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields: user_id, title, body' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Validate FCM server key
    if (!FCM_SERVER_KEY) {
      console.error('FCM_SERVER_KEY environment variable not set')
      return new Response(
        JSON.stringify({ error: 'Server configuration error' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Create Supabase client
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? ''
    )

    // Fetch user's FCM token and notification settings from database
    const { data: userData, error: userError } = await supabaseClient
      .from('users')
      .select('fcm_token, settings')
      .eq('id', user_id)
      .single()

    if (userError) {
      console.error('Error fetching user:', userError)
      return new Response(
        JSON.stringify({ error: 'User not found' }),
        { status: 404, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Check if user has FCM token
    if (!userData.fcm_token) {
      console.log('User has no FCM token, skipping notification:', user_id)
      return new Response(
        JSON.stringify({ message: 'User has no FCM token' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Check notification preferences
    const settings = userData.settings || {}
    if (settings.notificationsEnabled === false) {
      console.log('Notifications disabled for user:', user_id)
      return new Response(
        JSON.stringify({ message: 'Notifications disabled for user' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Check quiet hours
    if (settings.quietHoursEnabled === true) {
      const now = new Date()
      const currentHour = now.getHours()
      const startHour = settings.quietHoursStart || 22
      const endHour = settings.quietHoursEnd || 8

      const isQuietHours = startHour < endHour
        ? currentHour >= startHour || currentHour < endHour
        : currentHour >= startHour && currentHour < endHour

      if (isQuietHours) {
        console.log('Quiet hours active for user:', user_id)
        return new Response(
          JSON.stringify({ message: 'Quiet hours active' }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        )
      }
    }

    // Prepare FCM payload
    const fcmPayload = {
      to: userData.fcm_token,
      notification: {
        title: title,
        body: body,
        sound: settings.sound !== false ? 'default' : undefined,
        badge: '1'
      },
      data: data || {},
      priority: 'high',
      content_available: true
    }

    // Send to FCM
    const fcmResponse = await fetch(FCM_URL, {
      method: 'POST',
      headers: {
        'Authorization': `key=${FCM_SERVER_KEY}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(fcmPayload)
    })

    const fcmResult = await fcmResponse.json()

    if (fcmResponse.ok && fcmResult.success === 1) {
      console.log('Successfully sent notification to user:', user_id)

      // Log notification to database (optional)
      await supabaseClient
        .from('notification_log')
        .insert({
          user_id: user_id,
          title: title,
          body: body,
          sent_at: new Date().toISOString(),
          status: 'sent'
        })
        .catch(err => console.error('Failed to log notification:', err))

      return new Response(
        JSON.stringify({
          message: 'Notification sent successfully',
          fcm_message_id: fcmResult.results?.[0]?.message_id
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    } else {
      console.error('FCM error:', fcmResult)

      // Log failure
      await supabaseClient
        .from('notification_log')
        .insert({
          user_id: user_id,
          title: title,
          body: body,
          sent_at: new Date().toISOString(),
          status: 'failed',
          error: JSON.stringify(fcmResult)
        })
        .catch(err => console.error('Failed to log notification:', err))

      return new Response(
        JSON.stringify({ error: 'Failed to send notification', details: fcmResult }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

  } catch (error) {
    console.error('Unexpected error:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error', details: error.message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})

/*
 * Test locally:
 * supabase functions serve send-notification
 *
 * curl -i --location --request POST 'http://localhost:54321/functions/v1/send-notification' \
 *   --header 'Authorization: Bearer YOUR_ANON_KEY' \
 *   --header 'Content-Type: application/json' \
 *   --data '{"user_id":"uuid","title":"Test","body":"Test notification"}'
 *
 * Deploy:
 * supabase functions deploy send-notification --project-ref YOUR_PROJECT_REF
 *
 * Set environment variables:
 * supabase secrets set FCM_SERVER_KEY=your_fcm_server_key
 */
