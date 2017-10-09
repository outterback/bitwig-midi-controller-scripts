package com.akai;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class midimixExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("380ccb5b-4acf-42fe-ab3d-b2f9c742e020");
   
   public midimixExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "midimix";
   }
   
   @Override
   public String getAuthor()
   {
      return "oscarutterback";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor()
   {
      return "akai";
   }
   
   @Override
   public String getHardwareModel()
   {
      return "midimix";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 3;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      if (platformType == PlatformType.WINDOWS)
      {
         // TODO: Set the correct names of the ports for auto detection on Windows platform here
         // and uncomment this when port names are correct.
         // list.add(new String[]{"Input Port 0"}, new String[]{"Output Port 0"});
      }
      else if (platformType == PlatformType.MAC)
      {
         // TODO: Set the correct names of the ports for auto detection on Windows platform here
         // and uncomment this when port names are correct.
         // list.add(new String[]{"Input Port 0"}, new String[]{"Output Port 0"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         // TODO: Set the correct names of the ports for auto detection on Windows platform here
         // and uncomment this when port names are correct.
         // list.add(new String[]{"Input Port 0"}, new String[]{"Output Port 0"});
      }
   }

   @Override
   public midimixExtension createInstance(final ControllerHost host)
   {
      return new midimixExtension(this, host);
   }
}
